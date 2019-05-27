package org.openlumify.core.model.thumbnails;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlumify.core.simpleorm.SimpleOrmTestHelper;

@RunWith(MockitoJUnitRunner.class)
public class SimpleOrmThumbnailRepositoryTest extends ThumbnailRepositoryTestBase {
    private SimpleOrmThumbnailRepository artifactThumbnailRepository;

    @Override
    public void before() throws Exception {
        super.before();
        SimpleOrmTestHelper helper = new SimpleOrmTestHelper(getAuthorizationRepository());
        artifactThumbnailRepository = new SimpleOrmThumbnailRepository(
                helper.getSimpleOrmContextProvider(),
                getOntologyRepository(),
                helper.getSimpleOrmSession()
        );
    }

    @Override
    public SimpleOrmThumbnailRepository getThumbnailRepository() {
        return artifactThumbnailRepository;
    }
}
