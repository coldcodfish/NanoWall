package space.gorogoro.nanowall;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

public class NanoWallTask extends BukkitRunnable {

  private Material m;
  private Block b;
  private BlockData bd;

  @Override
  public void run() {
    b.setType(m);
    b.setBlockData(bd);
  }

  public BukkitRunnable setOriginal(Material m, Block b, BlockData bd) {
    this.m = m;
    this.b = b;
    this.bd = bd;

    return this;
  }
}

