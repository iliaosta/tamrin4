package rps.server.module.abstraction;

import rps.server.module.model.PlayerInfo;

import java.util.List;

public interface ISessionManager {

    /**
     * هر پیام دریافتی از UDP میاد اینجا.
     * خروجی: لیست پیام‌هایی که باید به کلاینت‌ها ارسال بشه
     */
    List<OutboundMessage> onMessage(PlayerInfo player, String message);

    /**
     * این متد باید به صورت دوره‌ای (مثلاً هر 1 ثانیه) توسط ServerService صدا زده شود
     * تا اگر session ای تایم‌اوت شد، پیام‌های لازم تولید شود.
     */
    List<OutboundMessage> checkTimeouts();

    record OutboundMessage(PlayerInfo to, String message) {}
}
