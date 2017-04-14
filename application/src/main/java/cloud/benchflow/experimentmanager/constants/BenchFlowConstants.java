package cloud.benchflow.experimentmanager.constants;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 2017-03-21
 */
public class BenchFlowConstants {

    private static final String YAML_EXTENSION = ".yml";

    // Minio
    public static final String DEPLOYMENT_DESCRIPTOR_NAME = "docker-compose";
    public static final String PT_PE_DEFINITION_NAME = "benchflow-test";
    public static final String BPMN_MODELS_FOLDER_NAME = "models";
    public static final String GENERATED_BENCHMARK_FILENAME = "benchflow-benchmark.jar";
    public static final String FABAN_CONFIG_FILENAME = "run.xml";
    public static final String MINIO_ID_DELIMITER = "/";

    // TODO - is this correct with only one bucket and it is called tests? Maybe better then just 'benchflow'?
    public static final String TESTS_BUCKET = "tests";
    public static final String PT_PE_DEFINITION_FILE_NAME = PT_PE_DEFINITION_NAME + YAML_EXTENSION;
    public static final String DEPLOYMENT_DESCRIPTOR_FILE_NAME = DEPLOYMENT_DESCRIPTOR_NAME + YAML_EXTENSION;

    // MongoDB
    public static final String DB_NAME = "benchflow-experiment-manager";
    public static final String MODEL_ID_DELIMITER = ".";

    // Faban
    public static final String FABAN_ID_DELIMITER = "_";

    // TODO - put in common library so they can be handled by client
    // Exceptions
    public static final String INVALID_EXPERIMENT_ID_MESSAGE = "Invalid Experiment ID";

}
