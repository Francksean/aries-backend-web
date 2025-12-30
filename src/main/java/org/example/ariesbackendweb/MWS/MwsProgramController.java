package org.example.ariesbackendweb.MWS;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.ariesbackendweb.MWS.dtos.MwsProgramRequest;
import org.example.ariesbackendweb.MWS.dtos.MwsProgramResponse;
import org.example.ariesbackendweb.common.api.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


/**
 * Controller REST pour la gestion des programmes MWS.
 *
 * Ce controller expose une API complète et cohérente pour toutes les opérations
 * sur les programmes MWS. Il suit les conventions REST et retourne des codes
 * HTTP appropriés pour chaque situation.
 *
 * Organisation des endpoints :
 * - CRUD de base sur /api/mws/programs
 * - Actions spéciales (sync, reactivate) via des sous-ressources
 * - Récupération dynamique des templates via des paramètres de query
 */
@Controller
@RequestMapping("/mws/program")
@Slf4j
public class MwsProgramController {

    @Autowired
    private MwsProgramService programService;
    @Autowired
    private AgentService agentService;

    /**
     * Crée un nouveau programme MWS.
     *
     * POST /api/mws/programs
     *
     * Ce endpoint permet à un utilisateur de définir un nouveau programme MWS
     * en spécifiant ses métadonnées de base, ses endpoints par environnement,
     * et optionnellement ses tags. Si autoSync est à true (valeur par défaut),
     * le système synchronisera automatiquement les opérations depuis le WSDL.
     *
     * Exemple de body JSON :
     * {
     *   "programName": "CustomerService",
     *   "description": "Service de gestion des clients",
     *   "programType": "API",
     *   "endpoints": {
     *     "DEV": {
     *       "wsdlUrl": "https://m3-dev:13080/mws/services/CustomerService?wsdl",
     *       "endpointUrl": "https://m3-dev:13080/mws/services/CustomerService"
     *     },
     *     "TEST": {
     *       "wsdlUrl": "https://m3-test:13080/mws/services/CustomerService?wsdl",
     *       "endpointUrl": "https://m3-test:13080/mws/services/CustomerService"
     *     }
     *   },
     *   "tags": ["CRM", "Master Data"],
     *   "autoSync": true,
     *   "syncEnvironment": "DEV"
     * }
     *
     * @param request Les informations du programme à créer
     * @return Le programme créé avec un code HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<MwsProgramResponse> createProgram(
            @Valid @RequestBody MwsProgramRequest request) {

        log.info("Requête de création de programme reçue: {}", request.getProgramName());

        try {
            MwsProgramResponse response = programService.createProgram(request);
            log.info("Programme créé avec succès: {} (ID: {})",
                    response.getProgramName(), response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Erreur de validation métier (ex: nom déjà existant)
            log.error("Erreur de validation lors de la création du programme", e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            // Erreur technique inattendue
            log.error("Erreur technique lors de la création du programme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère un programme par son identifiant.
     *
     * GET /api/mws/programs/{id}
     *
     * Ce endpoint retourne toutes les informations d'un programme incluant
     * ses endpoints, ses tags, et la liste de ses opérations (noms uniquement).
     *
     * @param id L'UUID du programme
     * @return Le programme demandé ou 404 si non trouvé
     */
    @GetMapping("/{id}")
    public ResponseEntity<MwsProgramResponse> getProgram(@PathVariable UUID id) {
        log.debug("Requête de récupération du programme: {}", id);

        try {
            MwsProgramResponse response = programService.getProgram(id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Programme non trouvé
            log.warn("Programme non trouvé: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du programme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Liste tous les programmes actifs.
     *
     * GET /api/mws/programs
     *
     * Ce endpoint retourne la liste de tous les programmes marqués comme actifs.
     * Chaque programme dans la liste contient toutes ses informations complètes.
     *
     * @return La liste des programmes actifs
     */
    @GetMapping
    public ResponseEntity<List<MwsProgramResponse>> listPrograms() {
        log.debug("Requête de liste des programmes");

        try {
            List<MwsProgramResponse> programs = programService.listPrograms();
            log.info("{} programme(s) retourné(s)", programs.size());
            return ResponseEntity.ok(programs);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la liste des programmes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Recherche des programmes par tag.
     *
     * GET /api/mws/programs?tag=CRM
     *
     * Ce endpoint permet de filtrer les programmes par tag, ce qui est très
     * utile pour organiser et retrouver rapidement des groupes de programmes.
     *
     * @param tag Le tag à rechercher
     * @return La liste des programmes ayant ce tag
     */
    @GetMapping(params = "tag")
    public ResponseEntity<List<MwsProgramResponse>> findProgramsByTag(
            @RequestParam String tag) {

        log.debug("Requête de recherche par tag: {}", tag);

        try {
            List<MwsProgramResponse> programs = programService.findProgramsByTag(tag);
            log.info("{} programme(s) trouvé(s) avec le tag '{}'", programs.size(), tag);
            return ResponseEntity.ok(programs);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Met à jour un programme existant.
     *
     * PUT /api/mws/programs/{id}
     *
     * Ce endpoint permet de modifier les informations d'un programme.
     * Notez que le nom du programme ne peut pas être changé pour maintenir
     * la cohérence des références.
     *
     * @param id L'UUID du programme à mettre à jour
     * @param request Les nouvelles informations du programme
     * @return Le programme mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<MwsProgramResponse> updateProgram(
            @PathVariable UUID id,
            @Valid @RequestBody MwsProgramRequest request) {

        log.info("Requête de mise à jour du programme: {}", id);

        try {
            MwsProgramResponse response = programService.updateProgram(id, request);
            log.info("Programme mis à jour avec succès: {}", id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Programme non trouvé pour mise à jour: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du programme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprime (désactive) un programme.
     *
     * DELETE /api/mws/programs/{id}
     *
     * Cette opération ne supprime pas physiquement le programme de la base
     * de données. Elle le marque simplement comme inactif, ce qui permet
     * de conserver l'historique et de potentiellement le réactiver plus tard.
     *
     * @param id L'UUID du programme à supprimer
     * @return 204 No Content si succès, 404 si non trouvé
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        log.info("Requête de suppression du programme: {}", id);

        try {
            programService.deleteProgram(id);
            log.info("Programme supprimé avec succès: {}", id);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.warn("Programme non trouvé pour suppression: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du programme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Réactive un programme précédemment désactivé.
     *
     * POST /api/mws/programs/{id}/reactivate
     *
     * Ce endpoint permet de réactiver un programme qui avait été supprimé
     * logiquement, sans avoir à le recréer complètement.
     *
     * @param id L'UUID du programme à réactiver
     * @return 200 OK si succès, 404 si non trouvé
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateProgram(@PathVariable UUID id) {
        log.info("Requête de réactivation du programme: {}", id);

        try {
            programService.reactivateProgram(id);
            log.info("Programme réactivé avec succès: {}", id);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.warn("Programme non trouvé pour réactivation: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors de la réactivation du programme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Synchronise les opérations d'un programme depuis son WSDL.
     *
     * POST /api/mws/programs/{id}/sync
     *
     * Ce endpoint déclenche une synchronisation légère qui récupère uniquement
     * les noms des opérations disponibles dans le WSDL. Cette synchronisation
     * peut être nécessaire après qu'un administrateur MWS a modifié le service
     * (ajout, suppression, ou renommage d'opérations).
     *
     * L'environnement spécifié détermine quel WSDL sera utilisé pour la
     * synchronisation. Par défaut, c'est l'environnement DEV.
     *
     * @param id L'UUID du programme à synchroniser
     * @param environment L'environnement source (DEV, TEST, PROD)
     * @return 200 OK si succès, avec des détails sur la synchronisation
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<String> syncOperations(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "DEV") String environment) {

        log.info("Requête de synchronisation pour le programme {} (env: {})", id, environment);

        try {
            agentService.syncOperations(id, environment);
            String message = "Synchronisation réussie pour l'environnement " + environment;
            log.info(message);
            return ResponseEntity.ok(message);

        } catch (IllegalArgumentException e) {
            // Programme ou environnement non trouvé
            log.error("Erreur de validation lors de la synchronisation", e);
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (RuntimeException e) {
            // Erreur de communication avec l'agent ou le WSDL
            log.error("Erreur lors de la synchronisation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la synchronisation: " + e.getMessage());
        }
    }

    /**
     * Récupère le template SOAP pour une opération spécifique.
     *
     * GET /api/mws/programs/{id}/template?environment=DEV&operation=GetBasicData
     *
     * Ce endpoint représente le cœur de notre approche dynamique. Au lieu de
     * retourner un template pré-stocké, il demande à l'agent de le générer
     * en temps réel depuis le WSDL actuel. Cela garantit que l'utilisateur
     * obtient toujours un template à jour reflétant la version courante du service.
     *
     * Le template retourné est un XML SOAP complet avec des placeholders (?)
     * que l'utilisateur devra remplir avec les valeurs de test appropriées.
     *
     * @param id L'UUID du programme
     * @param environment L'environnement cible
     * @param operationName Le nom de l'opération
     * @return Le template SOAP en XML
     */
    @GetMapping("/{id}/template")
    public ResponseEntity<String> getOperationTemplate(
            @PathVariable UUID id,
            @RequestParam String environment,
            @RequestParam String operationName) {

        log.info("Requête de template pour {}/{}/{}", id, environment, operationName);

        try {
            String template = agentService.getOperationTemplate(id, environment, operationName);
            log.debug("Template retourné: {} caractères", template.length());
            return ResponseEntity.ok(template);

        } catch (IllegalArgumentException e) {
            // Programme, environnement ou opération non trouvé
            log.error("Erreur de validation lors de la récupération du template", e);
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (RuntimeException e) {
            // Erreur de communication avec l'agent ou génération de template
            log.error("Erreur lors de la génération du template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la génération du template: " + e.getMessage());
        }
    }
}
