package com.mrkirby153.kcuhc.game;

import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manager for in-game events. Handles announcements, scheduling, and execution of events
 */
public class EventTracker implements Runnable {

    private final AtomicLong eventId = new AtomicLong(0);
    /**
     * A list of events scheduled
     */
    private final List<QueuedEvent> events = new CopyOnWriteArrayList<>();
    private final JavaPlugin plugin;

    private boolean running = false;
    private int runningTaskId = -1;

    private long notifyTick = 0;
    private long lastNagSecond = 0;
    private long lastNagTask = -1;

    public EventTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the executor
     */
    public void start() {
        if (running) {
            return;
        }
        runningTaskId = plugin.getServer().getScheduler()
            .scheduleSyncRepeatingTask(plugin, this, 0, 1);
        running = true;
    }

    /**
     * Stops the event executor and resets the queue
     */
    public void stop() {
        if (this.runningTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(this.runningTaskId);
        }
        reset(true);
        running = false;
    }

    /**
     * Resets the game ticks and optionally clears the queue
     *
     * @param clearQueue If the queue should be cleared
     */
    public void reset(boolean clearQueue) {
        if (clearQueue) {
            events.clear();
        }
    }

    private long schedule(ScheduledEvent event, long time, TimeUnit timeUnit, boolean async) {
        long ms = timeUnit.toMillis(time);
        long eventId = this.eventId.getAndIncrement();
        events.add(new QueuedEvent(eventId, event, System.currentTimeMillis() + ms, async));
        plugin.getLogger().info(event.getName() + " in " + Time.format(1,
            timeUnit.toMillis(time)));
        return eventId;
    }

    /**
     * Schedules an event to be executed synchronously
     *
     * @param event    The event to schedule
     * @param time     The time to schedule
     * @param timeUnit The time unit
     *
     * @return The id of the scheduled event
     */
    public long scheduleSyncEvent(ScheduledEvent event, long time, TimeUnit timeUnit) {
        return schedule(event, time, timeUnit, false);
    }

    /**
     * Schedules an event to be executed <b>asynchronously</b>
     *
     * @param event    The event to schedule
     * @param time     The time to schedule
     * @param timeUnit The time units
     *
     * @return The id of the scheduled event
     */
    public long scheduleAsyncEvent(ScheduledEvent event, long time, TimeUnit timeUnit) {
        return schedule(event, time, timeUnit, true);
    }

    /**
     * Cancels an event
     *
     * @param eventId The even to cancel
     */
    public void cancel(long eventId) {
        for (QueuedEvent e : events) {
            if (e.getId() == eventId) {
                try {
                    e.getEvent().onCancel();
                } catch (Exception ex) {
                    // Ignore
                } finally {
                    events.remove(e);
                }
            }
        }
    }

    @Override
    public void run() {
        this.events.removeIf(QueuedEvent::isRan);
        List<QueuedEvent> toRun = this.events.stream()
            .filter(event -> event.getExecutionTime() < System.currentTimeMillis() && !event.isRan()).collect(
                Collectors.toList());
        toRun.forEach(event -> {
            Bukkit.getServer().getScheduler().runTask(this.plugin, () -> {
                Sound toPlay = event.getEvent().startSound();
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (toPlay != null) {
                        p.playSound(p.getLocation(), toPlay, SoundCategory.MASTER, 1, 1);
                    }
                    p.sendMessage(
                        Chat.message("", "{event}", "{event}", event.getEvent().getName())
                            .toLegacyText());
                });
            });
            if (event.async) {
                Bukkit.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    try {
                        event.getEvent().run();
                    } catch (Exception e) {
                        plugin.getLogger()
                            .log(Level.SEVERE, "An error occurred running event " + event.getId(),
                                e);
                    } finally {
                        event.run();
                    }
                });
            } else {
                Bukkit.getServer().getScheduler().runTask(this.plugin, () -> {
                    try {
                        event.getEvent().run();
                    } catch (Exception e) {
                        plugin.getLogger()
                            .log(Level.SEVERE, "An error occurred running event " + event.getId(),
                                e);
                    } finally {
                        event.run();
                    }
                });
            }
        });

        notifyTick++;
        if(notifyTick % 20 == 0) {
            getNextEvent().ifPresent(e -> {
                long roundMsLeft = 1000 * (e.getMsLeft() / 1000);
                if (e.getEvent().shouldAnnounce(roundMsLeft) && roundMsLeft >= 1000
                    || lastNagTask != e.getId()) {
                    long currSecond = Math.round(
                        System.currentTimeMillis() / 1000D); // Don't nag more than once a second
                    if (currSecond == lastNagSecond) {
                        return;
                    }
                    lastNagSecond = currSecond;
                    Sound toPlay = e.getEvent().notifySound();
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        if (toPlay != null) {
                            p.playSound(p.getLocation(), toPlay, SoundCategory.MASTER, 1, 1);
                        }
                        p.sendMessage(
                            Chat.message("", "{event} in {time}", "{event}", e.getEvent().getName(),
                                "{time}",
                                Time.format(1, roundMsLeft, Time.TimeUnit.FIT, Time.TimeUnit.SECONDS)).toLegacyText());
                    });
                    lastNagTask = e.getId();
                }
            });
        }
    }

    /**
     * Gets the next event
     *
     * @return An optional containing the next event
     */
    public Optional<QueuedEvent> getNextEvent() {
        if (events.size() == 0) {
            return Optional.empty();
        }
        QueuedEvent nextEvent = events.get(0);
        for (QueuedEvent e : this.events) {
            if (e.getMsLeft() < nextEvent.getMsLeft()) {
                nextEvent = e;
            }
        }
        return Optional.of(nextEvent);
    }


    /**
     * Data class for a scheduled event
     */
    public class QueuedEvent {

        private final long id;
        private final ScheduledEvent event;
        private final long executionTime;
        private final boolean async;

        private boolean ran = false;

        private QueuedEvent(long id, ScheduledEvent event, long ticks, boolean async) {
            this.id = id;
            this.event = event;
            this.executionTime = ticks;
            this.async = async;
        }

        public long getId() {
            return id;
        }

        public ScheduledEvent getEvent() {
            return event;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public boolean isAsync() {
            return async;
        }

        public void run() {
            this.ran = true;
        }

        public boolean isRan() {
            return ran;
        }

        public long getMsLeft() {
            return this.executionTime - System.currentTimeMillis();
        }
    }
}
