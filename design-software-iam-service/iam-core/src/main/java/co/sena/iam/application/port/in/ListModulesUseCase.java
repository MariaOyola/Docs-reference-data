package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.RbacModule;
import java.util.List;

/** HU-IAM-004 E1 — GET /modules. */
public interface ListModulesUseCase {

    List<RbacModule> listModules();
}
