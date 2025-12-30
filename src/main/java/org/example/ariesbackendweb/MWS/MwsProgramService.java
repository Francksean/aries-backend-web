package org.example.ariesbackendweb.MWS;

import org.example.ariesbackendweb.MWS.dtos.MwsProgramRequest;
import org.example.ariesbackendweb.MWS.dtos.MwsProgramResponse;
import org.example.ariesbackendweb.MWS.entities.MwsOperation;
import org.example.ariesbackendweb.MWS.entities.MwsProgram;
import org.example.ariesbackendweb.MWS.entities.MwsProgramEndpoint;
import org.example.ariesbackendweb.MWS.entities.MwsProgramTag;
import org.example.ariesbackendweb.MWS.repositories.MwsOperationRepository;
import org.example.ariesbackendweb.MWS.repositories.MwsProgramEndpointRepository;
import org.example.ariesbackendweb.MWS.repositories.MwsProgramRepository;
import org.example.ariesbackendweb.MWS.repositories.MwsProgramTagRepository;
import org.example.ariesbackendweb.common.api.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service principal pour la gestion des programmes MWS dans l'approche hybride.
 * <p>
 * Cette implémentation adopte une philosophie pragmatique où nous stockons en base
 * de données uniquement ce qui est nécessaire pour la performance et la traçabilité,
 * tout en générant dynamiquement les informations qui changent fréquemment ou qui
 * sont volumineuses.
 * <p>
 * Responsabilités :
 * - Gérer le CRUD complet des programmes MWS et leurs endpoints
 * - Synchroniser légèrement les noms d'opérations depuis les WSDL
 * - Servir de proxy intelligent vers l'agent pour les templates SOAP dynamiques
 * - Fournir une API cohérente au controller en masquant la complexité
 */
@Service
public class MwsProgramService {

    private static final Logger logger = LoggerFactory.getLogger(MwsProgramService.class);

    @Autowired
    private MwsProgramRepository programRepository;

    @Autowired
    private MwsProgramEndpointRepository endpointRepository;

    @Autowired
    private MwsOperationRepository operationRepository;

    @Autowired
    private MwsProgramTagRepository tagRepository;

    @Autowired
    AgentService agentService;

    /**
     * Crée un nouveau programme MWS avec synchronisation optionnelle.
     * Cette méthode illustre parfaitement l'approche hybride. Nous créons d'abord
     * le programme et ses endpoints en base de données, ce qui constitue les métadonnées
     * essentielles. Ensuite, si l'utilisateur le demande via le flag autoSync, nous
     * déclenchons une synchronisation légère qui ne récupère que les noms des opérations,
     * pas leurs détails complets.
     * L'intérêt de cette approche est que si la synchronisation échoue, le programme
     * reste créé et l'utilisateur peut relancer la synchronisation manuellement plus tard.
     * Nous ne faisons pas échouer toute la création juste parce que le WSDL est
     * temporairement inaccessible.
     */
    @Transactional
    public MwsProgramResponse createProgram(MwsProgramRequest request) {
        logger.info("Création d'un nouveau programme MWS: {}", request.getProgramName());

        // Validation métier : unicité du nom de programme
        if (programRepository.findByProgramName(request.getProgramName()).isPresent()) {
            logger.warn("Tentative de création d'un programme avec un nom déjà existant: {}",
                    request.getProgramName());
            throw new IllegalArgumentException(
                    "Un programme avec le nom '" + request.getProgramName() + "' existe déjà");
        }

        // Construction de l'entité programme avec ses propriétés de base
        MwsProgram program = new MwsProgram();
        program.setCode(request.getProgramName());
        program.setDescription(request.getDescription());
        program.setProgramType(request.getProgramType());
        program.setActive(true);

        // Sauvegarde initiale pour générer l'UUID
        // Cet UUID sera utilisé pour les relations avec les endpoints et les tags
        program = programRepository.save(program);
        logger.debug("Programme créé avec l'ID: {}", program.getId());

        // Ajout des endpoints pour chaque environnement
        // Ces endpoints sont cruciaux car ils contiennent les URLs WSDL qui seront
        // utilisées pour toutes les interactions futures avec les services MWS
        if (request.getEndpoints() != null && !request.getEndpoints().isEmpty()) {
            for (Map.Entry<String, MwsProgramRequest.EndpointInfo> entry :
                    request.getEndpoints().entrySet()) {

                setProgramEndpoints(entry, program);
            }
        }

        // Ajout des tags pour faciliter la recherche et l'organisation
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                MwsProgramTag tag = MwsProgramTag.builder()
                        .program(program)
                        .tag(tagName.trim().toUpperCase()) // Normalisation des tags
                        .build();
                program.getTags().add(tag);
            }
            logger.debug("{} tag(s) ajouté(s)", request.getTags().size());
        }

        // Sauvegarde finale avec toutes les relations
        program = programRepository.save(program);

        // Synchronisation automatique si demandée
        // Notez que nous attrapons les exceptions ici pour ne pas faire échouer
        // la création du programme si la synchronisation échoue
        if (request.isAutoSync()) {
            logger.info("Synchronisation automatique demandée pour l'environnement: {}",
                    request.getSyncEnvironment());
            try {
                agentService.syncOperations(program.getId(), request.getSyncEnvironment());
                logger.info("Synchronisation automatique réussie");
            } catch (Exception e) {
                logger.error("La synchronisation automatique a échoué, mais le programme a été créé. " +
                        "L'utilisateur pourra relancer la synchronisation manuellement.", e);
                // On pourrait aussi ajouter un flag dans la réponse pour indiquer
                // que la synchronisation a échoué
            }
        }

        return convertToResponse(program);
    }

    private static void setProgramEndpoints(Map.Entry<String, MwsProgramRequest.EndpointInfo> entry, MwsProgram program) {
        String environment = entry.getKey();
        MwsProgramRequest.EndpointInfo endpointInfo = entry.getValue();

        MwsProgramEndpoint endpoint = new MwsProgramEndpoint();
        endpoint.setProgram(program);
        endpoint.setEnvironment(environment);
        endpoint.setWsdlUrl(endpointInfo.getWsdlUrl());
        endpoint.setEndpointUrl(endpointInfo.getEndpointUrl());

        program.getEndpoints().add(endpoint);

        logger.debug("Endpoint ajouté - Env: {}, WSDL: {}",
                environment, endpointInfo.getWsdlUrl());
    }

    /**
     * Récupère un programme par son identifiant avec toutes ses relations.
     * <p>
     * Cette méthode utilise l'annotation @Transactional en lecture seule pour
     * optimiser les performances. Spring sait alors qu'aucune écriture ne sera
     * effectuée et peut faire certaines optimisations.
     */
    @Transactional(readOnly = true)
    public MwsProgramResponse getProgram(UUID programId) {
        logger.debug("Récupération du programme avec l'ID: {}", programId);

        MwsProgram program = programRepository.findById(programId)
                .orElseThrow(() -> {
                    logger.warn("Programme non trouvé: {}", programId);
                    return new IllegalArgumentException(
                            "Aucun programme trouvé avec l'ID: " + programId);
                });

        return convertToResponse(program);
    }

    /**
     * Liste tous les programmes MWS actifs.
     * <p>
     * Dans un système de production réel, vous voudriez probablement ajouter
     * de la pagination ici pour gérer efficacement un grand nombre de programmes.
     * Vous pourriez utiliser Spring Data JPA Pageable pour cela.
     */
    @Transactional(readOnly = true)
    public List<MwsProgramResponse> listPrograms() {
        logger.debug("Récupération de la liste de tous les programmes actifs");

        List<MwsProgram> programs = programRepository.findAllByActiveTrue();

        logger.info("{} programme(s) actif(s) trouvé(s)", programs.size());

        return programs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recherche des programmes par tag.
     * <p>
     * Cette méthode permet à votre UI d'offrir une fonctionnalité de filtrage
     * par catégorie, ce qui est très utile quand vous avez beaucoup de programmes.
     */
    @Transactional(readOnly = true)
    public List<MwsProgramResponse> findProgramsByTag(String tag) {
        logger.debug("Recherche de programmes avec le tag: {}", tag);

        List<MwsProgram> programs = programRepository.findByTags_Tag(tag.toUpperCase());

        logger.info("{} programme(s) trouvé(s) pour le tag '{}'", programs.size(), tag);

        return programs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour un programme existant.
     * <p>
     * Cette méthode adopte une stratégie de remplacement pour les endpoints et les tags.
     * Nous supprimons les anciennes relations et recréons les nouvelles. Une approche
     * plus sophistiquée pourrait faire un merge intelligent pour minimiser les opérations
     * de base de données, mais pour la clarté et la simplicité, le remplacement complet
     * est souvent préférable.
     */
    @Transactional
    public MwsProgramResponse updateProgram(UUID programId, MwsProgramRequest request) {
        logger.info("Mise à jour du programme: {}", programId);

        MwsProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Programme non trouvé avec l'ID: " + programId));

        // Mise à jour des propriétés de base
        // Notez que nous ne permettons pas de changer le nom du programme
        // car cela pourrait casser des références ailleurs dans le système
        program.setDescription(request.getDescription());
        program.setProgramType(request.getProgramType());

        // Remplacement complet des endpoints
        // Nous supprimons d'abord toutes les anciennes relations
        program.getEndpoints().clear();

        if (request.getEndpoints() != null && !request.getEndpoints().isEmpty()) {
            for (Map.Entry<String, MwsProgramRequest.EndpointInfo> entry :
                    request.getEndpoints().entrySet()) {

                setProgramEndpoints(entry, program);
            }
        }

        // Remplacement complet des tags
        program.getTags().clear();

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                MwsProgramTag tag = MwsProgramTag.builder()
                        .program(program)
                        .tag(tagName.trim().toUpperCase())
                        .build();
                program.getTags().add(tag);
            }
        }

        program = programRepository.save(program);

        logger.info("Programme mis à jour avec succès: {}", programId);

        return convertToResponse(program);
    }

    /**
     * Supprime logiquement un programme en le désactivant.
     * <p>
     * Nous ne supprimons jamais physiquement les programmes de la base de données
     * pour plusieurs raisons importantes. D'abord, cela préserve l'intégrité
     * référentielle avec les tables de tests qui pointent vers ce programme.
     * Ensuite, cela maintient l'historique pour l'audit. Enfin, cela permet
     * une récupération facile en cas d'erreur.
     */
    @Transactional
    public void deleteProgram(UUID programId) {
        logger.info("Désactivation du programme: {}", programId);

        MwsProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Programme non trouvé avec l'ID: " + programId));

        program.setActive(false);
        programRepository.save(program);

        logger.info("Programme désactivé avec succès: {}", programId);
    }

    /**
     * Réactive un programme précédemment désactivé.
     * <p>
     * Cette méthode permet de récupérer un programme supprimé par erreur
     * sans avoir à le recréer complètement.
     */
    @Transactional
    public void reactivateProgram(UUID programId) {
        logger.info("Réactivation du programme: {}", programId);

        MwsProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Programme non trouvé avec l'ID: " + programId));

        program.setActive(true);
        programRepository.save(program);

        logger.info("Programme réactivé avec succès: {}", programId);
    }


    /**
     * Convertit une entité MwsProgram en DTO pour l'API REST.
     * <p>
     * Cette méthode de conversion est importante car elle définit exactement
     * quelles informations sont exposées à l'extérieur via l'API. Elle sert
     * aussi de couche d'isolation entre votre modèle de données interne et
     * votre API publique, ce qui vous permet de changer l'un sans affecter l'autre.
     */
    private MwsProgramResponse convertToResponse(MwsProgram program) {
        MwsProgramResponse response = new MwsProgramResponse();

        response.setId(program.getId().toString());
        response.setProgramName(program.getCode());
        response.setDescription(program.getDescription());
        response.setProgramType(program.getProgramType());

        // TODO : c'est bizarre un peu, à regarder.
//        response.setActive(program.get());
        response.setCreatedAt(program.getCreatedAt());
        response.setLastSyncedAt(program.getLastSyncedAt());

        // Conversion des endpoints en map pour faciliter l'accès par environnement
        Map<String, MwsProgramResponse.EndpointInfo> endpointMap = new HashMap<>();
        for (MwsProgramEndpoint endpoint : program.getEndpoints()) {
            MwsProgramResponse.EndpointInfo endpointInfo =
                    new MwsProgramResponse.EndpointInfo();
            endpointInfo.setWsdlUrl(endpoint.getWsdlUrl());
            endpointInfo.setEndpointUrl(endpoint.getEndpointUrl());
            endpointMap.put(endpoint.getEnvironment(), endpointInfo);
        }
        response.setEndpoints(endpointMap);

        // Conversion des tags en liste simple
        List<String> tags = program.getTags().stream()
                .map(MwsProgramTag::getTag)
                .sorted() // Tri alphabétique pour la cohérence
                .collect(Collectors.toList());
        response.setTags(tags);

        // Conversion des opérations en résumés légers
        // Notez que nous ne retournons que les noms, pas les templates
        List<String> operations = program.getOperations().stream()
                .map(MwsOperation::getOperationName)
                .sorted() // Tri alphabétique pour faciliter la recherche
                .collect(Collectors.toList());
        response.setOperations(operations);

        return response;
    }
}