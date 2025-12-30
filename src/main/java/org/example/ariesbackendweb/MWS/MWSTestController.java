package org.example.ariesbackendweb.MWS;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.ariesbackendweb.MWS.dtos.MwsTestResultResponse;
import org.example.ariesbackendweb.MWS.dtos.MwsTestStatusResponse;
import org.example.ariesbackendweb.MWS.dtos.MwsTestSubmissionRequest;
import org.example.ariesbackendweb.MWS.dtos.MwsTestSubmissionResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;


/**
 * Controller REST pour la gestion de l'exécution des tests MWS.
 *
 * Ce controller expose une API complète pour soumettre des tests,
 * suivre leur avancement, et récupérer leurs résultats. Il suit
 * les conventions REST avec des codes HTTP appropriés et une
 * documentation claire de chaque endpoint.
 *
 * Architecture du flux utilisateur :
 * 1. POST /api/mws/tests - Soumettre un nouveau test
 * 2. GET /api/mws/tests/{id}/status - Suivre l'avancement (optionnel, WebSocket préféré)
 * 3. GET /api/mws/tests/{id} - Récupérer les résultats complets
 * 4. GET /api/mws/tests/my-tests - Lister l'historique de ses tests
 */
@Controller
@RequestMapping("/mws/tests")
@Slf4j
public class MWSTestController {

    @Autowired
    private MwsTestService testService;

    /**
     * Soumet un nouveau test MWS pour exécution.
     *
     * POST /api/mws/tests
     *
     * Cet endpoint est le point d'entrée principal pour lancer un test.
     * Il retourne immédiatement avec un code HTTP 202 Accepted et un UUID
     * qui permet de suivre l'avancement du test.
     *
     * L'utilisateur sera notifié via WebSocket quand le test sera terminé.
     * Il pourra alors appeler GET /api/mws/tests/{id} pour récupérer
     * les résultats complets.
     *
     * Exemple de body JSON :
     * {
     *   "programId": "123e4567-e89b-12d3-a456-426614174000",
     *   "environment": "TEST",
     *   "operationName": "GetBasicData",
     *   "requestBody": "<GetBasicDataRequest>...</GetBasicDataRequest>",
     *   "m3Username": "M3USER",
     *   "m3Password": "password",
     *   "timeoutMillis": 60000
     * }
     *
     * @param request Les paramètres du test à exécuter
     * @return Une réponse contenant l'UUID du test soumis
     */
    @PostMapping
    public ResponseEntity<MwsTestSubmissionResponse> submitTest(
            @Valid @RequestBody MwsTestSubmissionRequest request) {

        log.info("Requête de soumission de test reçue pour le programme {} / {}",
                request.getProgramId(), request.getOperationName());

        try {
            MwsTestSubmissionResponse response = testService.submitTest(request);

            log.info("Test soumis avec succès: {}", response.getTestId());

            // Code HTTP 202 Accepted : la requête a été acceptée pour traitement
            // mais le traitement n'est pas terminé
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IllegalArgumentException e) {
            // Erreur de validation métier (programme inexistant, etc.)
            log.error("Erreur de validation lors de la soumission du test", e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            // Erreur technique inattendue
            log.error("Erreur technique lors de la soumission du test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère le statut actuel d'un test.
     *
     * GET /api/mws/tests/{id}/status
     *
     * Cet endpoint permet de consulter l'état d'avancement d'un test.
     * Bien que les notifications WebSocket soient préférées pour les mises
     * à jour temps réel, cet endpoint reste utile pour récupérer le statut
     * à un instant T, par exemple quand l'utilisateur recharge la page.
     *
     * @param id L'UUID du test
     * @return Le statut actuel du test avec les métadonnées
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<MwsTestStatusResponse> getTestStatus(@PathVariable UUID id) {
        log.debug("Requête de statut pour le test: {}", id);

        try {
            MwsTestStatusResponse response = testService.getTestStatus(id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Test non trouvé: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du statut", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les résultats complets d'un test terminé.
     *
     * GET /api/mws/tests/{id}
     *
     * Cet endpoint retourne tous les détails d'un test une fois qu'il
     * est terminé : la requête SOAP envoyée, la réponse reçue, les
     * métriques de performance, et les éventuels messages d'erreur.
     *
     * Si le test n'est pas encore terminé, l'endpoint retourne une
     * erreur 409 Conflict.
     *
     * @param id L'UUID du test
     * @return Les résultats complets du test
     */
    @GetMapping("/{id}")
    public ResponseEntity<MwsTestResultResponse> getTestResult(@PathVariable UUID id) {
        log.info("Requête de résultat pour le test: {}", id);

        try {
            MwsTestResultResponse response = testService.getTestResult(id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Test non trouvé: {}", id);
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            // Test pas encore terminé
            log.info("Tentative de récupération des résultats d'un test non terminé: {}", id);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Error-Message", e.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des résultats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Liste tous les tests de l'utilisateur connecté.
     *
     * GET /api/mws/tests/my-tests
     *
     * Cet endpoint permet à l'utilisateur de consulter l'historique
     * de tous les tests qu'il a lancés, avec leur statut actuel.
     * Les tests sont triés par date de création décroissante (les plus
     * récents en premier).
     *
     * @param auth L'authentification Spring Security (injectée automatiquement)
     * @return La liste des tests de l'utilisateur
     */
//    @GetMapping("/my-tests")
//    public ResponseEntity<List<MwsTestStatusResponse>> listMyTests(Authentication auth) {
//        String username = auth != null ? auth.getName() : "dev-user";
//
//        log.debug("Requête de liste des tests pour l'utilisateur: {}", username);
//
//        try {
//            List<MwsTestStatusResponse> tests = testService.listUserTests(username);
//            log.info("{} test(s) trouvé(s) pour l'utilisateur {}", tests.size(), username);
//            return ResponseEntity.ok(tests);
//
//        } catch (Exception e) {
//            log.error("Erreur lors de la récupération de la liste des tests", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
}