package rocks.blackblock.bongocat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads and parses configuration files.
 * Supports both JSON and YAML formats.
 */
public class ConfigurationLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);

    private static final String DEFAULT_CONFIG_FILENAME = "bongocat.json";
    private static final String YAML_CONFIG_FILENAME = "bongocat.yaml";

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public ConfigurationLoader() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Load configuration from the default location.
     * Searches in order:
     * 1. bongocat.json in current directory
     * 2. bongocat.yaml in current directory
     * 3. ~/.config/bongocat/bongocat.json
     * 4. ~/.config/bongocat/bongocat.yaml
     * 5. Default configuration from resources
     *
     * @return loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public Configuration loadDefault() throws ConfigurationException {
        // Try current directory first
        Path jsonPath = Paths.get(DEFAULT_CONFIG_FILENAME);
        if (Files.exists(jsonPath)) {
            logger.info("Loading configuration from: {}", jsonPath);
            return loadFromPath(jsonPath);
        }

        Path yamlPath = Paths.get(YAML_CONFIG_FILENAME);
        if (Files.exists(yamlPath)) {
            logger.info("Loading configuration from: {}", yamlPath);
            return loadFromPath(yamlPath);
        }

        // Try ~/.config/bongocat/
        String home = System.getProperty("user.home");
        Path configDir = Paths.get(home, ".config", "bongocat");

        Path configJsonPath = configDir.resolve(DEFAULT_CONFIG_FILENAME);
        if (Files.exists(configJsonPath)) {
            logger.info("Loading configuration from: {}", configJsonPath);
            return loadFromPath(configJsonPath);
        }

        Path configYamlPath = configDir.resolve(YAML_CONFIG_FILENAME);
        if (Files.exists(configYamlPath)) {
            logger.info("Loading configuration from: {}", configYamlPath);
            return loadFromPath(configYamlPath);
        }

        // Fall back to default configuration
        logger.info("No configuration file found, using defaults");
        try {
            return loadFromResource("default-config.json");
        } catch (ConfigurationException e) {
            logger.warn("Could not load default-config.json resource, creating default configuration programmatically");
            return createDefault();
        }
    }

    /**
     * Load configuration from a specific file path
     *
     * @param path the configuration file path
     * @return loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public Configuration loadFromPath(Path path) throws ConfigurationException {
        if (!Files.exists(path)) {
            throw new ConfigurationException("Configuration file not found: " + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new ConfigurationException("Not a file: " + path);
        }

        try {
            Configuration config;
            String filename = path.getFileName().toString().toLowerCase();

            if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                config = yamlMapper.readValue(path.toFile(), Configuration.class);
            } else if (filename.endsWith(".json")) {
                config = jsonMapper.readValue(path.toFile(), Configuration.class);
            } else {
                // Try JSON first, then YAML
                try {
                    config = jsonMapper.readValue(path.toFile(), Configuration.class);
                } catch (IOException e) {
                    config = yamlMapper.readValue(path.toFile(), Configuration.class);
                }
            }

            // Validate the configuration
            config.validate();

            logger.info("Loaded configuration: {}", config);
            return config;

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from: " + path, e);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Invalid configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Load configuration from a specific file path string
     *
     * @param pathString the configuration file path as string
     * @return loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public Configuration loadFromPath(String pathString) throws ConfigurationException {
        return loadFromPath(Paths.get(pathString));
    }

    /**
     * Load configuration from a classpath resource
     *
     * @param resourceName the resource name (e.g., "default-config.json")
     * @return loaded configuration
     * @throws ConfigurationException if loading fails
     */
    public Configuration loadFromResource(String resourceName) throws ConfigurationException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new ConfigurationException("Resource not found: " + resourceName);
            }

            Configuration config;
            if (resourceName.endsWith(".yaml") || resourceName.endsWith(".yml")) {
                config = yamlMapper.readValue(is, Configuration.class);
            } else {
                config = jsonMapper.readValue(is, Configuration.class);
            }

            config.validate();
            return config;

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from resource: " + resourceName, e);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Invalid configuration in resource: " + e.getMessage(), e);
        }
    }

    /**
     * Save configuration to a file
     *
     * @param config the configuration to save
     * @param path the file path
     * @throws ConfigurationException if saving fails
     */
    public void save(Configuration config, Path path) throws ConfigurationException {
        try {
            // Create parent directories if needed
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            String filename = path.getFileName().toString().toLowerCase();

            if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                yamlMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), config);
            } else {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), config);
            }

            logger.info("Saved configuration to: {}", path);

        } catch (IOException e) {
            throw new ConfigurationException("Failed to save configuration to: " + path, e);
        }
    }

    /**
     * Create a default configuration instance
     *
     * @return default configuration
     */
    public static Configuration createDefault() {
        Configuration config = new Configuration();

        // Display settings
        config.setMonitorName(null); // Auto-detect
        config.setOverlayHeight(100);
        config.setOverlayPosition(rocks.blackblock.bongocat.platform.Position.TOP);
        config.setOverlayOpacity(255);
        config.setLayer(Configuration.LayerType.OVERLAY);

        // Cat appearance
        config.setCatHeight(80);
        config.setCatXOffset(0);
        config.setCatYOffset(0);
        config.setCatAlign(Configuration.Alignment.CENTER);
        config.setMirrorHorizontal(false);
        config.setMirrorVertical(false);

        // Animation
        config.setIdleFrame(0);
        config.setKeypressDurationMs(100);
        config.setFps(60);
        config.setEnableAntialiasing(true);

        // Input devices - empty list for auto-detection
        config.setKeyboardDevices(new java.util.ArrayList<>());

        // Test animation (disabled)
        config.setTestAnimationDurationMs(0);
        config.setTestAnimationIntervalMs(0);

        // Sleep mode (disabled)
        config.setEnableScheduledSleep(false);
        config.setSleepBegin(new rocks.blackblock.bongocat.config.Time(0, 0));
        config.setSleepEnd(new rocks.blackblock.bongocat.config.Time(6, 0));
        config.setIdleSleepTimeoutSec(0);

        // Debug
        config.setDebug(false);

        return config;
    }
}
