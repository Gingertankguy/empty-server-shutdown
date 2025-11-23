package net.Gingertankguy.emptyservershutdown;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigManager {

     private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
     private static final File CONFIG_FILE = new File("config/emptyservershutdown.json");

     public static EmptyServerShutdownConfig config;

     public static void load() {
          try {
               if (!CONFIG_FILE.exists()) {
                    generateDefaultConfig();
                    return;
               }

               FileReader reader = new FileReader(CONFIG_FILE);
               config = GSON.fromJson(reader, EmptyServerShutdownConfig.class);
               reader.close();

               if (config == null) {
                    EmptyServerShutdown.LOGGER.error("Config file was empty or invalid — regenerating.");
                    generateDefaultConfig();
               }

          } catch (Exception e) {
               EmptyServerShutdown.LOGGER.error("Failed to load config — regenerating defaults.", e);
               generateDefaultConfig();
          }
     }

     private static void generateDefaultConfig() {
          try {
               CONFIG_FILE.getParentFile().mkdirs();

               config = new EmptyServerShutdownConfig();

               FileWriter writer = new FileWriter(CONFIG_FILE);
               GSON.toJson(config, writer);
               writer.close();

               EmptyServerShutdown.LOGGER.info("Generated default config file at {}", CONFIG_FILE.getPath());

          } catch (IOException e) {
               EmptyServerShutdown.LOGGER.error("Failed to generate default config file!", e);
          }
     }
}