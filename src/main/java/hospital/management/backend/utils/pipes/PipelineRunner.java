package hospital.management.backend.utils.pipes;

import hospital.management.backend.config.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Chains multiple DataPipe steps into a single transformation pipeline.
 * Each step's output is the next step's input.
 *
 * Usage:
 *   PipelineRunner<String, String> pipeline = PipelineRunner
 *       .<String>start()
 *       .pipe(input -> input.strip())
 *       .pipe(input -> input.toLowerCase())
 *       .pipe(input -> input.replaceAll("\\s+", "_"));
 *
 *   String result = pipeline.run("  Hello World  ");  // "hello_world"
 */
public final class PipelineRunner<I, O> {

    private static final AppLogger logger = AppLogger.getLogger(PipelineRunner.class);

    @SuppressWarnings("rawtypes")
    private final List<DataPipe> steps = new ArrayList<>();

    private PipelineRunner() {}

    /** Start building a pipeline whose first step takes type T. */
    public static <T> PipelineRunner<T, T> start() {
        return new PipelineRunner<>();
    }

    /**
     * Adds a transformation step.
     * The output type R of this step becomes the input type of the next step.
     *
     * @param pipe the transformation to apply
     * @return a new PipelineRunner typed to the new output
     */
    @SuppressWarnings("unchecked")
    public <R> PipelineRunner<I, R> pipe(DataPipe<O, R> pipe) {
        steps.add(pipe);
        return (PipelineRunner<I, R>) this;
    }

    /**
     * Runs the full pipeline on the given input.
     *
     * @param input the value to transform
     * @return the final output after all steps
     * @throws Exception if any step throws
     */
    @SuppressWarnings("unchecked")
    public O run(I input) throws Exception {
        Object current = input;
        for (DataPipe step : steps) {
            current = step.process(current);
        }
        return (O) current;
    }

    /**
     * Runs the pipeline on every item in the list.
     * Items that throw are logged and skipped — partial results are returned.
     *
     * @param items input list
     * @return list of successfully transformed items
     */
    @SuppressWarnings("unchecked")
    public List<O> runAll(List<I> items) {
        List<O> results = new ArrayList<>(items.size());
        for (I item : items) {
            try {
                results.add(run(item));
            } catch (Exception e) {
                logger.warn("Pipeline step failed for item — skipped: " + e.getMessage());
            }
        }
        return results;
    }
}