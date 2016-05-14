package me.mrkirby153.kcuhc.arena;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

public class SpreadPlayersHandler {

    private static final Random random = new Random();

    public void execute(String worldName, double x, double z, double distance, double range) {
        System.out.println("Beginning spreadplayers");
        ArrayList<UHCTeam> t = TeamHandler.teams().stream().filter(te -> te != TeamHandler.spectatorsTeam()).collect(Collectors.toCollection(ArrayList<UHCTeam>::new));
        ArrayList<Player> players = new ArrayList<>();
        t.forEach(te -> te.getPlayers().stream().map(Bukkit::getPlayer).forEach(players::add));
        World world = null;

        world = Bukkit.getWorld(worldName);

        if (world == null) {
            return;
        } else {
            double xRangeMin = x - range;
            double zRangeMin = z - range;
            double xRangeMax = x + range;
            double zRangeMax = z + range;
            int spreadSize = this.getTeams(players);
            Location[] locations = this.getSpreadLocations(world, spreadSize, xRangeMin, zRangeMin, xRangeMax, zRangeMax);
            int rangeSpread = this.range(world, distance, xRangeMin, zRangeMin, xRangeMax, zRangeMax, locations);
            if (rangeSpread == -1) {
                System.err.println(String.format("Could not spread %d %s around %s,%s (too many players for space - try using spread of at most %s)", new Object[]{Integer.valueOf(spreadSize), "teams", Double.valueOf(x), Double.valueOf(z)}));
            } else {
                double distanceSpread = this.spread(world, players, locations, true);
                System.out.println(String.format("Succesfully spread %d %s around %s,%s", new Object[]{Integer.valueOf(locations.length), "teams", Double.valueOf(x), Double.valueOf(z)}));
                if (locations.length > 1) {
                    System.out.println(String.format("(Average distance between %s is %s blocks apart after %s iterations)", new Object[]{"teams", String.format("%.2f", new Object[]{Double.valueOf(distanceSpread)}), Integer.valueOf(rangeSpread)}));
                }
                return;
            }
        }
    }


    private int range(World world, double distance, double xRangeMin, double zRangeMin, double xRangeMax, double zRangeMax, Location[] locations) {
        boolean flag = true;

        int i;
        for (i = 0; i < 10000 && flag; ++i) {
            flag = false;
            double max = 3.4028234663852886E38D;

            int j;
            Location loc1;
            double z;
            double x;
            for (int locs = 0; locs < locations.length; ++locs) {
                Location i1 = locations[locs];
                j = 0;
                loc1 = new Location(world, 0.0D, 0.0D, 0.0D);

                for (int swap = 0; swap < locations.length; ++swap) {
                    if (locs != swap) {
                        Location loc3 = locations[swap];
                        z = i1.distanceSquared(loc3);
                        max = Math.min(z, max);
                        if (z < distance) {
                            ++j;
                            loc1.add(loc3.getX() - i1.getX(), 0.0D, 0.0D);
                            loc1.add(loc3.getZ() - i1.getZ(), 0.0D, 0.0D);
                        }
                    }
                }

                if (j > 0) {
                    i1.setX(i1.getX() / (double) j);
                    i1.setZ(i1.getZ() / (double) j);
                    x = Math.sqrt(loc1.getX() * loc1.getX() + loc1.getZ() * loc1.getZ());
                    if (x > 0.0D) {
                        loc1.setX(loc1.getX() / x);
                        i1.add(-loc1.getX(), 0.0D, -loc1.getZ());
                    } else {
                        z = xRangeMin >= xRangeMax ? xRangeMin : random.nextDouble() * (xRangeMax - xRangeMin) + xRangeMin;
                        double z1 = zRangeMin >= zRangeMax ? zRangeMin : random.nextDouble() * (zRangeMax - zRangeMin) + zRangeMin;
                        i1.setX(z);
                        i1.setZ(z1);
                    }

                    flag = true;
                }

                boolean var31 = false;
                if (i1.getX() < xRangeMin) {
                    i1.setX(xRangeMin);
                    var31 = true;
                } else if (i1.getX() > xRangeMax) {
                    i1.setX(xRangeMax);
                    var31 = true;
                }

                if (i1.getZ() < zRangeMin) {
                    i1.setZ(zRangeMin);
                    var31 = true;
                } else if (i1.getZ() > zRangeMax) {
                    i1.setZ(zRangeMax);
                    var31 = true;
                }

                if (var31) {
                    flag = true;
                }
            }

            if (!flag) {
                Location[] var29 = locations;
                int var30 = locations.length;

                for (j = 0; j < var30; ++j) {
                    loc1 = var29[j];
                    if (world.getHighestBlockYAt(loc1) == 0) {
                        x = xRangeMin >= xRangeMax ? xRangeMin : random.nextDouble() * (xRangeMax - xRangeMin) + xRangeMin;
                        z = zRangeMin >= zRangeMax ? zRangeMin : random.nextDouble() * (zRangeMax - zRangeMin) + zRangeMin;
                        locations[i] = new Location(world, x, 0.0D, z);
                        loc1.setX(x);
                        loc1.setZ(z);
                        flag = true;
                    }
                }
            }
        }

        return i >= 10000 ? -1 : i;
    }

    private double spread(World world, List<Player> list, Location[] locations, boolean teams) {
        double distance = 0.0D;
        int i = 0;
        HashMap hashmap = Maps.newHashMap();

        for (int j = 0; j < list.size(); ++j) {
            Player player = (Player) list.get(j);
            if(player == null)
                continue;
            Location location;
            if (teams) {
                Team team = player.getScoreboard().getPlayerTeam(player);
                if (!hashmap.containsKey(team)) {
                    hashmap.put(team, locations[i++]);
                }

                location = (Location) hashmap.get(team);
            } else {
                location = locations[i++];
            }

            player.teleport(new Location(world, Math.floor(location.getX()) + 0.5D, (double) world.getHighestBlockYAt((int) location.getX(), (int) location.getZ()), Math.floor(location.getZ()) + 0.5D));
            double value = 1.7976931348623157E308D;

            for (int k = 0; k < locations.length; ++k) {
                if (location != locations[k]) {
                    double d = location.distanceSquared(locations[k]);
                    value = Math.min(d, value);
                }
            }

            distance += value;
        }

        distance /= (double) list.size();
        return distance;
    }

    private int getTeams(List<Player> players) {
        HashSet teams = Sets.newHashSet();
        Iterator var3 = players.iterator();

        while (var3.hasNext()) {
            Player player = (Player) var3.next();
            if(player == null)
                continue;
            UHCTeam teamForPlayer = TeamHandler.getTeamForPlayer(player);
            teams.add(teamForPlayer);
        }

        return teams.size();
    }

    private Location[] getSpreadLocations(World world, int size, double xRangeMin, double zRangeMin, double xRangeMax, double zRangeMax) {
        Location[] locations = new Location[size];

        for (int i = 0; i < size; ++i) {
            double x = xRangeMin >= xRangeMax ? xRangeMin : random.nextDouble() * (xRangeMax - xRangeMin) + xRangeMin;
            double z = zRangeMin >= zRangeMax ? zRangeMin : random.nextDouble() * (zRangeMax - zRangeMin) + zRangeMin;
            locations[i] = new Location(world, x, 0.0D, z);
        }

        return locations;
    }
}
