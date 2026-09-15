package com.tieyoh.api.feed.dto;

// One structured entry in the Feed. Deliberately not a preformatted
// sentence -- the spec is explicit that the backend should hand back
// structured data and let each client render it, not just push down
// "John sent Susan $50" as a string. Extensible by construction: a new
// event type is a new set of nullable fields (or, once there are enough
// of them, a generic payload map) plus a new case in FeedService's
// aggregation -- nothing about this shape or FeedController changes.
// Only the fields relevant to a given eventType are populated; the rest
// are left null.
public class FeedItemResult {

    private String eventType;
    private String eventId;
    // Epoch millis, not a formatted string -- the mobile app's minSdk
    // predates safe java.time usage without desugaring (which isn't
    // enabled), so a plain Long that Android's own DateUtils can render
    // avoids any client-side date-string parsing entirely.
    private Long occurredAt;

    // GIFT_ACTIVITY
    private PersonSummary sender;
    private PersonSummary recipient;
    private GiftInfo gift;
    private MerchantSummary merchant;

    // UPCOMING_BIRTHDAY
    private PersonSummary person;
    private Integer daysUntil;

    // UPCOMING_HOLIDAY
    private String holidayName;
    private String displayMessage;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Long occurredAt) {
        this.occurredAt = occurredAt;
    }

    public PersonSummary getSender() {
        return sender;
    }

    public void setSender(PersonSummary sender) {
        this.sender = sender;
    }

    public PersonSummary getRecipient() {
        return recipient;
    }

    public void setRecipient(PersonSummary recipient) {
        this.recipient = recipient;
    }

    public GiftInfo getGift() {
        return gift;
    }

    public void setGift(GiftInfo gift) {
        this.gift = gift;
    }

    public MerchantSummary getMerchant() {
        return merchant;
    }

    public void setMerchant(MerchantSummary merchant) {
        this.merchant = merchant;
    }

    public PersonSummary getPerson() {
        return person;
    }

    public void setPerson(PersonSummary person) {
        this.person = person;
    }

    public Integer getDaysUntil() {
        return daysUntil;
    }

    public void setDaysUntil(Integer daysUntil) {
        this.daysUntil = daysUntil;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }
}
