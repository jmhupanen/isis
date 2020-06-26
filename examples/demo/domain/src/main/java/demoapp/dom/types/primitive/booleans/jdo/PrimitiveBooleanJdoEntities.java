package demoapp.dom.types.primitive.booleans.jdo;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import org.apache.isis.applib.services.repository.RepositoryService;

@Service
public class PrimitiveBooleanJdoEntities {

    public Optional<PrimitiveBooleanJdoEntity> find(final boolean readOnlyProperty) {
        return repositoryService.firstMatch(PrimitiveBooleanJdoEntity.class, x -> x.isReadOnlyProperty() == readOnlyProperty);
    }

    public List<PrimitiveBooleanJdoEntity> all() {
        return repositoryService.allInstances(PrimitiveBooleanJdoEntity.class);
    }

    @Inject
    RepositoryService repositoryService;

}
