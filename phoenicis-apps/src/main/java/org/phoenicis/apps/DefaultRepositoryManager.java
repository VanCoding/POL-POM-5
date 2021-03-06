package org.phoenicis.apps;

import org.phoenicis.apps.dto.ApplicationDTO;
import org.phoenicis.apps.dto.CategoryDTO;
import org.phoenicis.apps.dto.ScriptDTO;
import org.phoenicis.tools.ToolsConfiguration;
import org.phoenicis.tools.files.FileUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by marc on 31.03.17.
 */
public class DefaultRepositoryManager implements RepositoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRepositoryManager.class);

    private final LocalRepository.Factory localRepositoryFactory;
    private final ClasspathRepository.Factory classPathRepositoryFactory;
    private final String cacheDirectoryPath;

    private final FileUtilities fileUtilities;

    private MultipleRepository multipleRepository;
    private FilterRepository filterRepository;
    private CachedRepository cachedRepository;
    private BackgroundRepository backgroundRepository;

    private List<CallbackPair> callbacks;

    public DefaultRepositoryManager(ExecutorService executorService, boolean enforceUncompatibleOperatingSystems, ToolsConfiguration toolsConfiguration, String cacheDirectoryPath, FileUtilities fileUtilities, LocalRepository.Factory localRepositoryFactory, ClasspathRepository.Factory classPathRepositoryFactory) {
        super();

        this.localRepositoryFactory = localRepositoryFactory;
        this.classPathRepositoryFactory = classPathRepositoryFactory;
        this.cacheDirectoryPath = cacheDirectoryPath;
        this.fileUtilities = fileUtilities;

        this.callbacks = new ArrayList<CallbackPair>();

        this.multipleRepository = new MultipleRepository();
        this.filterRepository = new FilterRepository(multipleRepository, toolsConfiguration.operatingSystemFetcher(), enforceUncompatibleOperatingSystems);
        this.cachedRepository = new CachedRepository(filterRepository);
        this.backgroundRepository = new BackgroundRepository(cachedRepository, executorService);
    }

    @Override
    public void addCallbacks(Consumer<List<CategoryDTO>> onRepositoryChange, Consumer<Exception> onError) {
        this.callbacks.add(new CallbackPair(onRepositoryChange, onError));
    }

    public ApplicationDTO getApplication(List<String> path) {
        return this.cachedRepository.getApplication(path);
    }

    public ScriptDTO getScript(List<String> path) {
        return this.cachedRepository.getScript(path);
    }

    public void moveRepository(String repositoryUrl, int toIndex) {
        LOGGER.info(String.format("Move repository: %s to %d", repositoryUrl, toIndex));
        this.multipleRepository.moveRepository(toRepository(repositoryUrl), toIndex);
        this.triggerRepositoryChange();
    }

    public void addRepositories(int index, String ... repositoryUrls) {
        LOGGER.info(String.format("Adding repositories: %s at index %d", Arrays.toString(repositoryUrls), index));
        for (int repositoryUrlIndex = 0; repositoryUrlIndex < repositoryUrls.length; repositoryUrlIndex++) {
            Repository repository = toRepository(repositoryUrls[repositoryUrlIndex]);

            this.multipleRepository.addRepository(index + repositoryUrlIndex, repository);
        }
        this.triggerRepositoryChange();
    }

    public void addRepositories(String ... repositoryUrls) {
        this.addRepositories(this.multipleRepository.size(), repositoryUrls);
    }

    public void removeRepositories(String ... repositoryUrls) {
        LOGGER.info(String.format("Removing repositories: %s", Arrays.toString(repositoryUrls)));

        List<Repository> toDeleteRepositories = Arrays.stream(repositoryUrls).map(this::toRepository).collect(Collectors.toList());
        toDeleteRepositories.forEach(this.multipleRepository::removeRepository);

        this.triggerRepositoryChange();

        toDeleteRepositories.forEach(Repository::onDelete);
    }

    public void triggerRepositoryChange() {
        this.cachedRepository.clearCache();

        if (!this.callbacks.isEmpty()) {
            this.backgroundRepository.fetchInstallableApplications(
                    categories -> this.callbacks.forEach(callbackPair -> callbackPair.getOnRepositoryChange().accept(categories)),
                    exception -> this.callbacks.forEach(callbackPair -> callbackPair.getOnError().accept(exception)));
        }
    }

    private Repository toRepository(String repositoryUrl) {
        LOGGER.info("Converting: " + repositoryUrl + " to Repository");
        try {
            final URI url = new URI(repositoryUrl);
            final String scheme = url.getScheme().split("\\+")[0];

            switch (scheme) {
                case "git":
                    return new GitRepository(repositoryUrl.replace("git+",""), cacheDirectoryPath,
                            localRepositoryFactory, fileUtilities);
                case "file":
                    return localRepositoryFactory.createInstance(url.getRawPath());
                case "classpath":
                    return classPathRepositoryFactory.createInstance(url.getPath());
                default:
                    LOGGER.warn("Unsupported URL: " + repositoryUrl);
                    return new NullRepository();
            }
        } catch (URISyntaxException e) {
            LOGGER.warn("Cannot parse URL: " + repositoryUrl, e);
            return new NullRepository();
        }
    }

    private class CallbackPair {
        private Consumer<List<CategoryDTO>> onRepositoryChange;

        private Consumer<Exception> onError;

        public CallbackPair(Consumer<List<CategoryDTO>> onRepositoryChange, Consumer<Exception> onError) {
            this.onRepositoryChange = onRepositoryChange;
            this.onError = onError;
        }

        public Consumer<List<CategoryDTO>> getOnRepositoryChange() {
            return this.onRepositoryChange;
        }

        public Consumer<Exception> getOnError() {
            return this.onError;
        }
    }
}
