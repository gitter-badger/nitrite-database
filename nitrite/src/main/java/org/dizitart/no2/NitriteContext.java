package org.dizitart.no2;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.dizitart.no2.fulltext.TextIndexingService;
import org.dizitart.no2.fulltext.TextTokenizer;
import org.dizitart.no2.internals.JacksonMapper;
import org.dizitart.no2.internals.NitriteMapper;
import org.dizitart.no2.util.ExecutorUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.dizitart.no2.util.ExecutorUtils.shutdownAndAwaitTermination;

/**
 * Represents a readonly view of all contextual information of a nitrite database.
 *
 * @author Anindya Chatterjee.
 * @since 1.0
 * @see Nitrite#getContext()
 */
@Getter @Setter(AccessLevel.PACKAGE)
public class NitriteContext {
    /**
     * Gets the database file path. For in-memory database
     * it returns `null`.
     *
     * @returns database file path.
     * */
    private String filePath;

    /**
     * Gets the size of the auto-commit buffer. If the buffer size
     * exceeds this value and auto-commit is on, nitrite will save
     * the changes to disk.
     *
     * @returns auto-commit buffer size.
     *
     * */
    private int autoCommitBufferSize;

    /**
     * Indicates if this is an in-memory database or not.
     *
     * @returns `true`, if in-memory; otherwise `false`.
     * */
    private boolean inMemory;

    /**
     * Indicates if this is a readonly database or not.
     *
     * @returns `true`, if readonly; otherwise `false`.
     * */
    private boolean readOnly;

    /**
     * Indicates if this is a compressed database or not.
     *
     * @returns `true`, if compressed; otherwise `false`.
     * */
    private boolean compressed;

    /**
     * Indicates if auto commit is enabled or not.
     *
     * @returns `true`, if auto commit is enabled; otherwise `false`.
     * */
    private boolean autoCommitEnabled;

    /**
     * Indicates if auto compact is enabled or not before close.
     *
     * @returns `true`, if auto compact is enabled; otherwise `false`.
     * */
    private boolean autoCompactEnabled;

    /**
     * Gets the custom {@link TextIndexingService} implementation used for the database.
     * It returns `null` if the default {@link TextIndexingService} implementation
     * is used.
     *
     * @returns a {@link TextIndexingService} instance or `null`.
     * @see org.dizitart.no2.filters.Filters#text(String, String)
     * @see org.dizitart.no2.objects.filters.ObjectFilters#text(String, String)
     * @see NitriteBuilder#textIndexingService(TextIndexingService)
     * */
    private TextIndexingService textIndexingService;

    /**
     * Gets the {@link TextTokenizer} implementation used for the database. It returns
     * `null` if the default {@link TextTokenizer} implementation is used.
     *
     * @returns a {@link TextTokenizer} instance or `null`.
     * @see org.dizitart.no2.filters.Filters#text(String, String)
     * @see org.dizitart.no2.objects.filters.ObjectFilters#text(String, String)
     * @see NitriteBuilder#textTokenizer(TextTokenizer)
     * */
    private TextTokenizer textTokenizer;

    @Setter(AccessLevel.NONE)
    private ExecutorService workerPool;

    @Setter(AccessLevel.NONE)
    private ScheduledExecutorService scheduledWorkerPool;

    @Getter(AccessLevel.NONE)
    private NitriteMapper nitriteMapper;

    @Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
    private List<String> collectionRegistry;

    @Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
    private List<Class<?>> repositoryRegistry;

    /**
     * Instantiates a new Nitrite context.
     */
    NitriteContext() {
        workerPool = ExecutorUtils.daemonExecutor();
        scheduledWorkerPool = ExecutorUtils.scheduledExecutor();
    }

    /**
     * Gets the {@link NitriteMapper} instance configured.
     *
     * @return the {@link NitriteMapper}.
     */
    public NitriteMapper getNitriteMapper() {
        if (nitriteMapper == null) {
            nitriteMapper = new JacksonMapper();
        }
        return nitriteMapper;
    }

    void shutdown() {
        shutdownAndAwaitTermination(scheduledWorkerPool, 5);
        shutdownAndAwaitTermination(workerPool, 5);
    }
}
