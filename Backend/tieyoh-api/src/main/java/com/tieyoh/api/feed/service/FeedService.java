package com.tieyoh.api.feed.service;

import com.tieyoh.api.auth.dto.FeedPersonResult;
import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.entity.FriendConnection;
import com.tieyoh.api.auth.repository.AppUserRepository;
import com.tieyoh.api.auth.repository.FriendConnectionRepository;
import com.tieyoh.api.auth.service.FriendService;
import com.tieyoh.api.feed.dto.FeedItemResult;
import com.tieyoh.api.feed.dto.FeedResponse;
import com.tieyoh.api.feed.dto.GiftInfo;
import com.tieyoh.api.feed.dto.MerchantSummary;
import com.tieyoh.api.feed.dto.PersonSummary;
import com.tieyoh.api.feed.entity.HolidayDefinition;
import com.tieyoh.api.feed.repository.HolidayDefinitionRepository;
import com.tieyoh.api.gift.dto.GiftCommentResult;
import com.tieyoh.api.gift.entity.Gift;
import com.tieyoh.api.gift.entity.GiftComment;
import com.tieyoh.api.gift.repository.GiftCommentRepository;
import com.tieyoh.api.gift.repository.GiftRepository;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.repository.MerchantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Backs GET /api/feed -- the "Interactive Social/Gifting Feed" spec.
// Aggregates from three sources today (GIFT_ACTIVITY, UPCOMING_BIRTHDAY,
// UPCOMING_HOLIDAY), each producing plain FeedItemResult rows that are
// merged, ranked, and paginated together. Adding a fourth event type
// later means one new private build*Items(...) method contributing to
// the same merged list -- nothing about the merge/rank/paginate pipeline
// or the public getFeed(...) contract needs to change.
//
// viewerId is trusted straight from the client (there's no session/auth
// context anywhere in this backend yet -- see tieyoh-auth-architecture.md),
// same trust model every other endpoint in this app already uses. The
// spec calls for resolving the viewer from an authenticated security
// context and never accepting one from the client; that's a real backend
// auth project of its own, out of scope here, and this endpoint is no
// less trustworthy than /api/users/{id}/network or /api/users/{id}/friends
// already are today.
@Service
public class FeedService {

    // Deliberately simple, fixed windows for MVP -- see the "simple
    // deterministic ranking mechanism" note below for why nothing fancier
    // than this is built yet.
    private static final int GIFT_LOOKBACK_DAYS = 30;
    private static final int BIRTHDAY_WINDOW_DAYS = 7;
    private static final int HOLIDAY_WINDOW_DAYS = 14;

    private final AppUserRepository appUserRepository;
    private final FriendConnectionRepository friendConnectionRepository;
    private final FriendService friendService;
    private final GiftRepository giftRepository;
    private final MerchantRepository merchantRepository;
    private final HolidayDefinitionRepository holidayDefinitionRepository;
    private final GiftCommentRepository giftCommentRepository;

    public FeedService(AppUserRepository appUserRepository,
                        FriendConnectionRepository friendConnectionRepository,
                        FriendService friendService,
                        GiftRepository giftRepository,
                        MerchantRepository merchantRepository,
                        HolidayDefinitionRepository holidayDefinitionRepository,
                        GiftCommentRepository giftCommentRepository) {
        this.appUserRepository = appUserRepository;
        this.friendConnectionRepository = friendConnectionRepository;
        this.friendService = friendService;
        this.giftRepository = giftRepository;
        this.merchantRepository = merchantRepository;
        this.holidayDefinitionRepository = holidayDefinitionRepository;
        this.giftCommentRepository = giftCommentRepository;
    }

    public FeedResponse getFeed(Long viewerId, int page, int size) {
        AppUser viewer = appUserRepository.findById(viewerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<RankedItem> all = new ArrayList<>();
        all.addAll(buildGiftActivityItems(viewer));
        all.addAll(buildBirthdayItems(viewer));
        all.addAll(buildHolidayItems());

        // "Simple deterministic ranking mechanism that can later be
        // replaced" per the spec -- everything shares one comparable
        // scale (roughly "hours away from now," negative-friendly isn't
        // needed since a gift's hoursAgo and a birthday/holiday's
        // hoursUntil are both always >= 0), so recent gift activity and
        // imminent birthdays/holidays interleave naturally instead of
        // being shown as separate blocks.
        all.sort(Comparator.comparingDouble(item -> item.rankScore));

        List<FeedItemResult> items = all.stream().map(item -> item.result).collect(Collectors.toList());

        int from = Math.min(Math.max(page, 0) * Math.max(size, 1), items.size());
        int to = Math.min(from + Math.max(size, 1), items.size());
        List<FeedItemResult> pageItems = items.subList(from, to);

        return new FeedResponse(pageItems, page, size, to < items.size());
    }

    // ---- GIFT_ACTIVITY ----

    private List<RankedItem> buildGiftActivityItems(AppUser viewer) {
        LocalDateTime since = LocalDateTime.now().minusDays(GIFT_LOOKBACK_DAYS);
        List<Gift> recentGifts = giftRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
        if (recentGifts.isEmpty()) {
            return List.of();
        }

        // The viewer's own existing "network" definition (direct circle +
        // second-degree) already established for the feed's network row
        // -- reused here as exactly what CONTACTS visibility means: the
        // gift is visible to anyone who shares that network with either
        // party, not just the two people directly involved.
        Set<Long> network = friendService.getNetwork(viewer.getId())
                .stream()
                .map(FeedPersonResult::getUserId)
                .collect(Collectors.toSet());
        network.add(viewer.getId());

        List<RankedItem> events = new ArrayList<>();
        for (Gift gift : recentGifts) {
            if (!isVisibleToViewer(gift, viewer.getId(), network)) {
                continue;
            }

            AppUser sender = appUserRepository.findById(gift.getSenderId()).orElse(null);
            AppUser recipient = appUserRepository.findById(gift.getRecipientId()).orElse(null);
            if (sender == null || recipient == null) {
                continue;
            }

            FeedItemResult item = new FeedItemResult();
            item.setEventType("GIFT_ACTIVITY");
            item.setEventId("gift-" + gift.getId());
            item.setOccurredAt(toEpochMillis(gift.getCreatedAt()));
            item.setSender(toPersonSummary(sender));
            item.setRecipient(toPersonSummary(recipient));
            List<GiftCommentResult> comments = giftCommentRepository
                    .findByGiftIdOrderByCreatedAtAsc(gift.getId())
                    .stream()
                    .map(FeedService::toCommentResult)
                    .collect(Collectors.toList());
            item.setGift(new GiftInfo(gift.getAmount(), gift.getCurrency(), comments));

            if (gift.getMerchantId() != null) {
                merchantRepository.findById(gift.getMerchantId())
                        .ifPresent(m -> item.setMerchant(toMerchantSummary(m)));
            }

            double hoursAgo = Duration.between(gift.getCreatedAt(), LocalDateTime.now()).toMinutes() / 60.0;
            events.add(new RankedItem(item, Math.max(hoursAgo, 0)));
        }
        return events;
    }

    // Financial transaction details must never be exposed just because
    // someone happens to have the sender or recipient in their contacts
    // -- this is the one authorization check every gift passes through
    // before it can appear in anyone's feed but the two people involved.
    private boolean isVisibleToViewer(Gift gift, Long viewerId, Set<Long> network) {
        if (gift.getSenderId().equals(viewerId) || gift.getRecipientId().equals(viewerId)) {
            return true;
        }
        if ("PUBLIC".equals(gift.getVisibility())) {
            return true;
        }
        if ("CONTACTS".equals(gift.getVisibility())) {
            return network.contains(gift.getSenderId()) || network.contains(gift.getRecipientId());
        }
        // PRIVATE, or an unrecognized value -- default closed.
        return false;
    }

    // ---- UPCOMING_BIRTHDAY ----

    private List<RankedItem> buildBirthdayItems(AppUser viewer) {
        if (viewer.getTieyohId() == null) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        List<RankedItem> events = new ArrayList<>();

        for (FriendConnection fc : friendConnectionRepository.findByOwnerTieyohIdOrderByDisplayNameAsc(viewer.getTieyohId())) {
            if (!"ACTIVE".equals(fc.getStatus()) || fc.getContactUserId() == null) {
                continue;
            }
            AppUser contact = appUserRepository.findById(fc.getContactUserId()).orElse(null);
            if (contact == null || contact.getBirthMonth() == null || contact.getBirthDay() == null) {
                continue;
            }

            LocalDate nextBirthday = nextOccurrence(today, contact.getBirthMonth(), contact.getBirthDay());
            long daysUntil = ChronoUnit.DAYS.between(today, nextBirthday);
            if (daysUntil > BIRTHDAY_WINDOW_DAYS) {
                continue;
            }

            FeedItemResult item = new FeedItemResult();
            item.setEventType("UPCOMING_BIRTHDAY");
            item.setEventId("birthday-" + contact.getId() + "-" + nextBirthday);
            item.setOccurredAt(toEpochMillis(nextBirthday.atStartOfDay()));
            item.setPerson(toPersonSummary(contact));
            item.setDaysUntil((int) daysUntil);

            events.add(new RankedItem(item, daysUntil * 24.0));
        }
        return events;
    }

    // Only this user's DIRECT circle is scanned for birthdays (not the
    // wider second-degree network used for gift visibility) -- a
    // birthday reminder for someone two hops away isn't the same kind of
    // relevant as it is for someone actually in your own circle.
    private LocalDate nextOccurrence(LocalDate today, int month, int day) {
        LocalDate thisYear = safeDate(today.getYear(), month, day);
        if (!thisYear.isBefore(today)) {
            return thisYear;
        }
        return safeDate(today.getYear() + 1, month, day);
    }

    // Clamps Feb 29 to Feb 28 in a non-leap year rather than throwing --
    // a birthday shouldn't only exist every four years.
    private LocalDate safeDate(int year, int month, int day) {
        int lastDayOfMonth = YearMonth.of(year, month).lengthOfMonth();
        return LocalDate.of(year, month, Math.min(day, lastDayOfMonth));
    }

    // ---- UPCOMING_HOLIDAY ----

    private List<RankedItem> buildHolidayItems() {
        LocalDate today = LocalDate.now();
        List<RankedItem> events = new ArrayList<>();

        for (HolidayDefinition holiday : holidayDefinitionRepository.findByEnabledTrue()) {
            LocalDate occurrence = holiday.resolveDateForYear(today.getYear());
            if (occurrence.isBefore(today)) {
                occurrence = holiday.resolveDateForYear(today.getYear() + 1);
            }
            long daysUntil = ChronoUnit.DAYS.between(today, occurrence);
            if (daysUntil > HOLIDAY_WINDOW_DAYS) {
                continue;
            }

            FeedItemResult item = new FeedItemResult();
            item.setEventType("UPCOMING_HOLIDAY");
            item.setEventId("holiday-" + holiday.getId() + "-" + occurrence);
            item.setOccurredAt(toEpochMillis(occurrence.atStartOfDay()));
            item.setHolidayName(holiday.getName());
            item.setDisplayMessage(holiday.getDisplayMessage());
            item.setDaysUntil((int) daysUntil);

            events.add(new RankedItem(item, daysUntil * 24.0));
        }
        return events;
    }

    // ---- shared mapping helpers ----

    // The prototype runs as a single-timezone deployment, so converting
    // through the JVM's own default zone is a reasonable simplification
    // here -- same correctness-first, don't-over-engineer-for-MVP
    // approach used elsewhere in this service.
    private long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private PersonSummary toPersonSummary(AppUser user) {
        return new PersonSummary(user.getId(), user.getDisplayName(), user.getTieyohId(), user.getProfilePictureBase64());
    }

    private MerchantSummary toMerchantSummary(Merchant merchant) {
        return new MerchantSummary(merchant.getId(), merchant.getTmid(), merchant.getBrandName(),
                merchant.getLogoUrl(), merchant.getCategory());
    }

    private static GiftCommentResult toCommentResult(GiftComment comment) {
        return new GiftCommentResult(
                comment.getId(),
                comment.getGiftId(),
                comment.getAuthorUserId(),
                comment.getAuthorDisplayName(),
                comment.getBody(),
                comment.getCreatedAt() == null ? null : comment.getCreatedAt().toString());
    }

    // Pairs a finished FeedItemResult with the sort key used to rank it
    // against every other event type before pagination -- kept out of
    // the DTO itself since it's a ranking implementation detail, not
    // something the client needs to see.
    private static final class RankedItem {
        private final FeedItemResult result;
        private final double rankScore;

        private RankedItem(FeedItemResult result, double rankScore) {
            this.result = result;
            this.rankScore = rankScore;
        }
    }
}
