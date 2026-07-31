package hospital.management.backend.utils.pipes;

/**
 * A single transformation step in a pipeline.
 * Input type T is consumed, output type R is produced.
 *
 * Pipe steps are composable — chain them inside PipelineRunner.
 *
 * Example:
 *   DataPipe<String, String> trimPipe  = input -> input.strip();
 *   DataPipe<String, String> upperPipe = input -> input.toUpperCase();
 */
@FunctionalInterface
public interface DataPipe<T, R> {
    R process(T input) throws Exception;
}