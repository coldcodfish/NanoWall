package space.gorogoro.nanowall;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NanoWall extends JavaPlugin implements Listener {

  @Override
  public void onEnable(){
    try{
      getLogger().info("The Plugin Has Been Enabled!");

      // If there is no setting file, it is created
      if(!getDataFolder().exists()){
        getDataFolder().mkdir();
      }

      File configFile = new File(getDataFolder(), "config.yml");
      if(!configFile.exists()){
        saveDefaultConfig();
      }

      final PluginManager pm = getServer().getPluginManager();
      pm.registerEvents(this, this);

    } catch (Exception e){
      e.printStackTrace();
    }
  }

  /**
   * JavaPlugin method onCommand.
   *
   * @return boolean true:Success false:Display the usage dialog set in plugin.yml
   */
  public boolean onCommand( CommandSender sender, Command commandInfo, String label, String[] args) {
    try{
      if(!commandInfo.getName().equals("nanowall")) {
        return false;
      }

      if(args.length <= 0) {
        return false;
      }
      String subCommand = args[0];

      String playerName;
      OfflinePlayer p = null;
      FileConfiguration config;
      List<String> memberList;

      switch(subCommand) {
        case "reload":
          if(!sender.isOp()) {
            return false;
          }
          reloadConfig();
          break;

        case "addmember":
          if(!sender.isOp()) {
            return false;
          }

          if(args.length != 2) {
            return false;
          }
          playerName =args[1];

          for(Player cur:getServer().getOnlinePlayers()) {
            if(cur.getName().equals(playerName)) {
              p = cur;
            }
          }
          if(p == null) {
            for(OfflinePlayer cur:getServer().getOfflinePlayers()) {
              if(cur.getName().equals(playerName)) {
                p = cur;
              }
            }
            if(p == null) {
              sender.sendMessage(ChatColor.GRAY + "プレイヤーがオンラインの時に追加してください。" + ChatColor.RESET);
              return true;
            }
          }
          config = getConfig();
          memberList = config.getStringList("member-list");
          if(memberList.contains(p.getUniqueId().toString()) == false) {
            memberList.add(p.getUniqueId().toString());
          }
          Collections.sort(memberList);
          config.set("member-list", memberList);
          Location l = new Location(p.getPlayer().getWorld(), 0, 0, 0);
          config.set("wall-area-start", l.getBlock().getLocation());
          saveConfig();
          sender.sendMessage(ChatColor.GRAY + "追加しました。" + ChatColor.RESET);
          break;

        case "delmember":
          if(!sender.isOp()) {
            return false;
          }

          if(args.length != 2) {
            return false;
          }
          playerName =args[1];

          for(Player cur:getServer().getOnlinePlayers()) {
            if(cur.getName().equals(playerName)) {
              p = cur;
            }
          }
          if(p == null) {
            for(OfflinePlayer cur:getServer().getOfflinePlayers()) {
              if(cur.getName().equals(playerName)) {
                p = cur;
              }
            }
            if(p == null) {
              sender.sendMessage(ChatColor.GRAY + "プレイヤーがオンラインの時に削除してください。" + ChatColor.RESET);
              return true;
            }
          }

          config = getConfig();
          memberList = config.getStringList("member-list");
          if(memberList.contains(p.getUniqueId().toString()) == true) {
            memberList.remove(p.getUniqueId().toString());
          }
          Collections.sort(memberList);
          config.set("member-list", memberList);
          saveConfig();
          sender.sendMessage(ChatColor.GRAY + "削除しました。" + ChatColor.RESET);
          break;


        case "listmember":
          if(!sender.isOp()) {
            return false;
          }

          if(args.length != 1) {
            return false;
          }

          config = getConfig();
          sender.sendMessage(ChatColor.WHITE + "ナノウォールメンバー一覧" + ChatColor.RESET);
          sender.sendMessage(ChatColor.WHITE + "＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝" + ChatColor.RESET);
          for(String s:config.getStringList("member-list")) {
            UUID uuid = UUID.fromString(s);
            if(getServer().getPlayer(uuid) != null ) {
              p = getServer().getPlayer(uuid);
            }
            if(p == null) {
              for(OfflinePlayer cur:getServer().getOfflinePlayers()) {
                if(cur.getUniqueId().equals(uuid)) {
                  p = cur;
                }
              }
            }

            if(p != null) {
              sender.sendMessage(ChatColor.GRAY + "  " + p.getName() + ChatColor.RESET);
            }
          }
          sender.sendMessage(ChatColor.WHITE + "＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝" + ChatColor.RESET);
          break;

        default:
          return false;
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    return true;
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();
    if( !isMember(p) ) {
      return;
    }

    for(Location loc:getNearByBlock(2, p.getLocation()) ){
      if(isNanoWall(loc)) {
        Block b = loc.getBlock();
        BlockData bd = b.getBlockData();
        Material m = b.getType();
        if(!b.getType().equals(Material.AIR)) {
          b.setType(Material.AIR);
          new NanoWallTask().setOriginal(m, b, bd).runTaskLater(this, 2 * 20L);
        }
      }
    }
  }

  private boolean isMember(Player p) {
    if(getConfig().getStringList("member-list").contains(p.getUniqueId().toString())) {
      return true;
    }
    return false;
  }

  public boolean isNanoWall(Location loc) {
    Location firstPoint = new Location(
      getServer().getWorld(getConfig().getString("wall-first-point.world")),
      getConfig().getDouble("wall-first-point.x"),
      getConfig().getDouble("wall-first-point.y"),
      getConfig().getDouble("wall-first-point.z")
    );
    Location secondPoint = new Location(
      getServer().getWorld(getConfig().getString("wall-second-point.world")),
      getConfig().getDouble("wall-second-point.x"),
      getConfig().getDouble("wall-second-point.y"),
      getConfig().getDouble("wall-second-point.z")
    );

    UUID worldUniqueId = firstPoint.getWorld().getUID();
    double maxX = Math.max(firstPoint.getX(), secondPoint.getX());
    double maxY = Math.max(firstPoint.getY(), secondPoint.getY());
    double maxZ = Math.max(firstPoint.getZ(), secondPoint.getZ());
    double minX = Math.min(firstPoint.getX(), secondPoint.getX());
    double minY = Math.min(firstPoint.getY(), secondPoint.getY());
    double minZ = Math.min(firstPoint.getZ(), secondPoint.getZ());

    return loc.getWorld().getUID().equals(worldUniqueId)
        && minX <= loc.getX() && loc.getX() <= maxX
        && minY <= loc.getY() && loc.getY() <= maxY
        && minZ <= loc.getZ() && loc.getZ() <= maxZ;
  }

  public ArrayList<Location> getNearByBlock(int radius, Location loc){
    ArrayList<Location> locs = new ArrayList<Location>();
    for(int i = -radius; i <= radius; i++) {
      for(int j = -radius; j <= radius; j++) {
        for(int k = -radius; k <= radius; k++) {
          locs.add(loc.getBlock().getRelative(i, j, k).getLocation());
        }
      }
    }
    return locs;
  }


  @Override
  public void onDisable(){
    try{
      getLogger().info("The Plugin Has Been Disabled!");
    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
