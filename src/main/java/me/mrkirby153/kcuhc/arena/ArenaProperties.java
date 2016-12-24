package me.mrkirby153.kcuhc.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.mrkirby153.kcuhc.UHC;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.lang.reflect.Field;

public class ArenaProperties {

    public transient String name;

    public Property<Boolean> SPREAD_PLAYERS = new Property<>("spread_players", true);
    public Property<Boolean> CHECK_ENDING = new Property<>("check_ending", true);

    public Property<Boolean> DROP_PLAYER_HEAD = new Property<>("drop_head", true);
    public Property<Boolean> ENABLE_HEAD_APPLE = new Property<>("head_apple", true);

    public Property<Integer> WORLDBORDER_START_SIZE = new Property<>("wb_max", 1500);
    public Property<Integer> WORLDBORDER_END_SIZE = new Property<>("wb_min", 60);
    public Property<Integer> WORLDBORDER_TRAVEL_TIME = new Property<>("wb_travel", 30);
    public Property<LocationProperty> WORLDBORDER_CENTER = new Property<>("wb_location", new LocationProperty(0, 0, 0));
    public Property<Boolean> ENABLE_WORLDBORDER_WARNING = new Property<>("wb_warn", true);
    public Property<Integer> WORLDBORDER_WARN_DISTANCE = new Property<>("wb_warn_dist", 50);

    public Property<String> WORLD = new Property<>("world", "world");

    public Property<Integer> MIN_DISTANCE_BETWEEN_TEAMS = new Property<>("min_distance", 50);

    public Property<Integer> REJOIN_MINUTES = new Property<>("rejoin_mins", 5);
    public Property<Boolean> ENABLE_ENDGAME = new Property<>("endgame_enable", true);

    public Property<Boolean> GIVE_COMPASS_ON_START = new Property<>("give_compass", true);
    public Property<Boolean> COMPASS_PLAYER_TRACKER = new Property<>("compass_tracks_players", true);

    public Property<Boolean> REGEN_TICKET_ENABLE = new Property<>("regen_ticket", true);

    public Property<Integer> PVP_GRACE_MINS = new Property<>("pvp_grace", 10);
    public Property<Boolean> TEAM_INV_ENABLED = new Property<>("team_inv", true);

    public Property<Integer> LONE_WOLF_TEAM_SIZE = new Property<>("lone_wolf_team_size", 2);
    public Property<Boolean> LONE_WOLF_CREATES_TEAMS = new Property<>("lone_wolf_creates_teams", false);

    public static ArenaProperties loadProperties(String fileName) {
        File file = new File(UHC.getInstance().getDataFolder(), "presets/" + fileName + ".json");
        if(!file.exists()){
            System.out.println("Preset "+fileName+" does not exist, creating");
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (Exception e){
                e.printStackTrace();
            }
            ArenaProperties p = new ArenaProperties();
            saveProperties(p, fileName);
            p.name = fileName;
            return p;
        }
        try {
            FileReader reader = new FileReader(file);
            ArenaProperties props = new Gson().fromJson(reader, ArenaProperties.class);
            props.name = fileName;
            reader.close();
            return props;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean propertyExists(String name){
        File file = new File(UHC.getInstance().getDataFolder(), "presets/" + name + ".json");
        return file.exists();
    }

    public static void saveProperties(ArenaProperties properties, String fileName) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String json = gson.toJson(properties);

        try {
            FileWriter writer = new FileWriter(new File(UHC.getInstance().getDataFolder(), "presets/" + fileName + ".json"));
            writer.write(json);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dumpProperties() {
        System.out.println("----------");
        for (Field f : this.getClass().getDeclaredFields()) {
            if (f.getType() == Property.class) {
                try {
                    Property p = (Property) f.get(this);
                    System.out.println(String.format("  - %s = %s", p.getName(), p.get()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("----------");
    }

    public void resetAllToDefault() {
        for (Field f : ArenaProperties.class.getDeclaredFields()) {
            if (f.getType() == Property.class) {
                try {
                    Property p = (Property) f.get(null);
                    p.reset();
                    System.out.println("Reset property " + p.getName() + " to default");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class Property<T> {

        private final T defaultValue;
        private String name;
        private T value;

        public Property(String name, T defaultValue) {
            this.name = name;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
        }

        public T get() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        public String getName() {
            return name;
        }

        public void reset() {
            this.value = defaultValue;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    public static class LocationProperty {

        private double x, y, z;

        public LocationProperty(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Location construct(World world) {
            return new Location(world, x, y, z);
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }
    }
}
