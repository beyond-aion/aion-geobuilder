package com.aionemu.geobuilder;

import com.aionemu.geobuilder.loaders.CgfLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Terrain {

  private static final int TERRAIN_CUTOUT_MARKER = -1;

  public final int heightmapXSize, heightmapYSize, heightmapUnitSize;
  public ShortBuffer heightmap;
  public byte[] materialIds;
  public ByteBuffer materials;

  public Terrain(int heightmapXSize, int heightmapYSize, int heightmapUnitSize) {
    this.heightmapXSize = heightmapXSize;
    this.heightmapYSize = heightmapYSize;
    this.heightmapUnitSize = heightmapUnitSize;
  }

  public void load(ByteBuffer landMapH32, boolean allTerrainMaterials) throws IOException {
    boolean hasMaterials = false;
    heightmap = ShortBuffer.allocate(heightmapXSize * heightmapYSize);
    materials = ByteBuffer.allocate(heightmap.capacity());
    for (int i = 0; i < heightmap.capacity(); i++) {
      short z = landMapH32.getShort();
      if (z == TERRAIN_CUTOUT_MARKER) {
        int x = i / heightmapYSize * heightmapUnitSize;
        int y = i % heightmapYSize * heightmapUnitSize;
        throw new IOException("Encountered reserved value " + z + " at x: " + x + " y: " + y);
      }
      int materialIndex = Byte.toUnsignedInt(landMapH32.get());
      boolean isTerrainCutout = materialIndex == 0x3F;
      byte materialId = isTerrainCutout ? 0 : materialIds[materialIndex];
      if (!allTerrainMaterials && !CgfLoader.isUseSkillMaterial(Byte.toUnsignedInt(materialId)))
        materialId = 0;
      if (materialId != 0 && !hasMaterials)
        hasMaterials = true;
      heightmap.put(isTerrainCutout ? TERRAIN_CUTOUT_MARKER : z);
      materials.put(materialId);
    }
    if (landMapH32.remaining() > 0) {
      throw new IOException("Terrain data was not fully read.");
    }
    heightmap.flip();
    if (!hasMaterials) {
      materials = null;
    } else {
      materials.flip();
    }
  }

  public Heightmap createHeightmap() {
    return new Heightmap(heightmap.array(), heightmapXSize, heightmapYSize, "");
  }

  public Heightmap createMaterialmap() {
    return new Heightmap(materials.array(), heightmapXSize, heightmapYSize, "_materials");
  }

  public static class Heightmap {
    private final BufferedImage image;
    private final String fileNameSuffix;
    public final List<String> fileNames = new ArrayList<>();

    private Heightmap(byte[] data, int xSize, int ySize, String fileNameSuffix) {
      // we need these cumbersome initializers if we want to avoid array copies
      ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] { 8 }, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
      WritableRaster raster = (WritableRaster) Raster.createRaster(colorModel.createCompatibleSampleModel(xSize, ySize), new DataBufferByte(data, data.length), null);
      this.image = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
      this.fileNameSuffix = fileNameSuffix;
    }

    private Heightmap(short[] data, int xSize, int ySize, String fileNameSuffix) {
      // we need these cumbersome initializers if we want to avoid array copies
      ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] { 16 }, false, true, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
      WritableRaster raster = (WritableRaster) Raster.createRaster(colorModel.createCompatibleSampleModel(xSize, ySize), new DataBufferUShort(data, data.length), null);
      this.image = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
      this.fileNameSuffix = fileNameSuffix;
    }

    public void write(Path outputFolder, boolean disableOxipng, Logger oxipngLogger) {
      List<String> fileNames = this.fileNames;
      String fileNameStart = fileNames.stream().sorted().collect(Collectors.joining(","));
      Path file = outputFolder.resolve(fileNameStart + fileNameSuffix + ".png");
      try {
        if (disableOxipng)
          ImageIO.write(image, "png", file.toFile());
        else
          writeOptimized(image, file, oxipngLogger);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void writeOptimized(BufferedImage image, Path file, Logger log) {
      String oxipng = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "./oxipng/oxipng.exe" : "./oxipng/oxipng";
      try {
        Process process = new ProcessBuilder(oxipng, "-o", "max", "--fast", "--nb", "-s", "--out", file.toString(), "-").start();
        try (OutputStream outputStream = process.getOutputStream()) {
          ImageIO.write(image, "png", outputStream);
        } catch (IOException ignored) { // ignore broken pipe errors here since we log error messages from the process
        }
        try (BufferedReader reader = process.errorReader()) {
          reader.lines().takeWhile(l -> process.isAlive()).forEach(l -> log.info("[oxipng] " + l + '\r'));
          int exitCode = process.waitFor();
          String result = reader.lines().collect(Collectors.joining("\n"));
          if (exitCode != 0)
            log.severe("oxipng process terminated with exit code " + exitCode + ": " + result);
          else if (!result.isEmpty())
            log.info("[oxipng] " + result + '\r');
        }
      } catch (IOException | InterruptedException e) {
        log.log(Level.SEVERE, "", e);
      }
    }
  }
}
