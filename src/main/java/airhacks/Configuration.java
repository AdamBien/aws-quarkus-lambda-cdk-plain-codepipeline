package airhacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import airhacks.logging.control.Logging;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;

public class Configuration {
    
    //useful e.g. for vpc lookups
    public String codeStarConnectionARN = "notset";
    //public String accountID;
    //public String region;



    static final String CONFIGURATION_FILE = "configuration.json";

    /**
     * create an empty configuration file if not exists
     * @param location checks the existence of a file in this location
     * @throws JsonbException on serialization error
     * @throws IOException on IO error
     */
    static void createConfigurationIfNotExists(Path location) throws JsonbException, IOException {
        if (!Files.exists(location)) {
            var config = new JsonbConfig().withNullValues(true).withFormatting(true);
            JsonbBuilder.create(config).toJson(new Configuration(),
                    Files.newOutputStream(location, StandardOpenOption.CREATE_NEW));
        }
    }

    /**
     * Optional additional configuration read from configuration.json Useful to
     * fetch VPC ids and subnet ids
     * 
     * @return parsed configuration
     * @throws IOException
     */
    public static Optional<Configuration> load() {
        var location = Path.of(CONFIGURATION_FILE);
        try {
            createConfigurationIfNotExists(location);
            var reader = Files.newBufferedReader(location);
            var configuration =  JsonbBuilder.create().fromJson(reader, Configuration.class);
            return Optional.of(configuration);
        } catch (Exception ex) {
            Logging.error("Cannot find configuration file " + CONFIGURATION_FILE);
            return Optional.empty();
        }
    }
}
