package org.reactome.server.tools

import org.biopax.paxtools.model.level3.CellularLocationVocabulary
import org.biopax.paxtools.model.level3.Dna
import org.biopax.paxtools.model.level3.DnaReference
import org.biopax.paxtools.model.level3.EntityFeature
import org.biopax.paxtools.model.level3.EntityReference
import org.biopax.paxtools.model.level3.FragmentFeature
import org.biopax.paxtools.model.level3.ModificationFeature
import org.biopax.paxtools.model.level3.PositionStatusType
import org.biopax.paxtools.model.level3.Protein
import org.biopax.paxtools.model.level3.ProteinReference
import org.biopax.paxtools.model.level3.Rna
import org.biopax.paxtools.model.level3.RnaReference
import org.biopax.paxtools.model.level3.SequenceEntityReference
import org.biopax.paxtools.model.level3.SequenceInterval
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary
import org.biopax.paxtools.model.level3.SequenceSite
import org.biopax.paxtools.model.level3.SimplePhysicalEntity
import org.biopax.paxtools.model.level3.SmallMolecule
import org.biopax.paxtools.model.level3.SmallMoleculeReference
import org.biopax.paxtools.model.level3.Stoichiometry
import org.biopax.paxtools.model.level3.UnificationXref
import org.reactome.server.graph.domain.model.AbstractModifiedResidue
import org.reactome.server.graph.domain.model.ChemicalDrug
import org.reactome.server.graph.domain.model.CrosslinkedResidue
import org.reactome.server.graph.domain.model.DatabaseObject
import org.reactome.server.graph.domain.model.Drug
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence
import org.reactome.server.graph.domain.model.FragmentInsertionModification
import org.reactome.server.graph.domain.model.FragmentModification
import org.reactome.server.graph.domain.model.GroupModifiedResidue
import org.reactome.server.graph.domain.model.ModifiedNucleotide
import org.reactome.server.graph.domain.model.ProteinDrug
import org.reactome.server.graph.domain.model.PsiMod
import org.reactome.server.graph.domain.model.RNADrug
import org.reactome.server.graph.domain.model.ReferenceDNASequence
import org.reactome.server.graph.domain.model.ReferenceEntity
import org.reactome.server.graph.domain.model.ReferenceGeneProduct
import org.reactome.server.graph.domain.model.ReferenceGroup
import org.reactome.server.graph.domain.model.ReferenceMolecule
import org.reactome.server.graph.domain.model.ReferenceRNASequence
import org.reactome.server.graph.domain.model.ReferenceSequence
import org.reactome.server.graph.domain.model.ReferenceTherapeutic
import org.reactome.server.graph.domain.model.ReplacedResidue
import org.reactome.server.graph.domain.model.SimpleEntity
import org.reactome.server.graph.domain.model.TranslationalModification
import org.biopax.paxtools.model.level3.Complex as BpComplex
import org.biopax.paxtools.model.level3.PhysicalEntity as BpPhysicalEntity
import org.reactome.server.graph.domain.model.Complex as RComplex
import org.reactome.server.graph.domain.model.PhysicalEntity as RPhysicalEntity

/**
 * A physical entity as the release's converter writes it (spec 001 T011), once per
 * file: its class from its reference entity's, its names, cellular location,
 * features, entity reference, the "Reactome DB_ID" comment, Reactome's two ids, the
 * data source, and its cross-references and diseases. A complex has its components,
 * each with its stoichiometry.
 *
 * A set is written as a PhysicalEntity with these properties and no members: its
 * class and members are T017's.
 */
class BioPAXPhysicalEntityBuilder(
    private val context: ConversionContext,
) {
    private val model = context.model
    private val xrefs = BioPAXXrefBuilder(context)

    fun physicalEntity(entity: RPhysicalEntity): BpPhysicalEntity =
        context.once("entity:${entity.dbId}") {
            val reference = referenceOf(entity)
            val bpEntity = model.addNew(classFor(entity, reference), context.nextId(classFor(entity, reference).simpleName))
            if (entity is Drug && reference != null) bpEntity.addComment("Converted from Drug in Reactome")
            BioPAX3Utils.addNames(entity.name, entity.displayName, bpEntity)
            addCellularLocation(entity, bpEntity)
            if (entity is RComplex) {
                addComponents(entity, bpEntity as BpComplex)
            } else if (reference != null && bpEntity is SimplePhysicalEntity) {
                bpEntity.entityReference = entityReference(reference, entity)
            }
            addModifications(entity, bpEntity)
            addFragment(entity, bpEntity)
            bpEntity.addComment("Reactome DB_ID: ${entity.dbId}")
            xrefs.addReactomeIds(entity, bpEntity)
            BioPAX3BasicElementsBuilder(null, context, bpEntity).addReactomeDataSource()
            // All of them: the release's converter stopped at the first it had
            // already written (an obvious bug, recorded in differences.md).
            entity.crossReference?.forEach { bpEntity.addXref(xrefs.additionalInformation(it)) }
            entity.disease?.forEach { bpEntity.addXref(xrefs.additionalInformation(it)) }
            bpEntity
        }

    // The reference entity that types an entity; a set's is T017's.
    private fun referenceOf(entity: RPhysicalEntity): ReferenceEntity? =
        when (entity) {
            is EntityWithAccessionedSequence -> entity.referenceEntity
            is SimpleEntity -> entity.referenceEntity
            is Drug -> entity.referenceEntity
            else -> null
        }

    private fun classFor(
        entity: RPhysicalEntity,
        reference: ReferenceEntity?,
    ): Class<out BpPhysicalEntity> =
        when {
            entity is RComplex -> BpComplex::class.java
            // A set has no reference here (T017 types it), so it is a PhysicalEntity too.
            reference == null -> BpPhysicalEntity::class.java
            entity is Drug ->
                when (entity) {
                    is ProteinDrug -> Protein::class.java
                    is ChemicalDrug -> SmallMolecule::class.java
                    is RNADrug -> Rna::class.java
                    else -> BpPhysicalEntity::class.java
                }
            reference is ReferenceGeneProduct -> Protein::class.java
            reference is ReferenceMolecule || reference is ReferenceGroup -> SmallMolecule::class.java
            reference is ReferenceDNASequence -> Dna::class.java
            reference is ReferenceRNASequence -> Rna::class.java
            else -> BpPhysicalEntity::class.java
        }

    // The first compartment, with its GO term as a UnificationXref; none without an accession.
    private fun addCellularLocation(
        entity: RPhysicalEntity,
        bpEntity: BpPhysicalEntity,
    ) {
        val compartment = entity.compartment?.firstOrNull() ?: return
        val accession = compartment.accession ?: return
        bpEntity.cellularLocation =
            context.once("location:${compartment.dbId}") {
                model.addNew(CellularLocationVocabulary::class.java, context.nextId("CellularLocationVocabulary")).apply {
                    compartment.name?.let { addTerm(it) }
                    addXref(
                        model.addNew(UnificationXref::class.java, context.nextId("UnificationXref")).apply {
                            db = "GENE ONTOLOGY"
                            id = if (accession.startsWith("GO:")) accession else "GO:$accession"
                        },
                    )
                }
            }
    }

    // Each component once, with a Stoichiometry of its count (a new one each time).
    private fun addComponents(
        complex: RComplex,
        bpComplex: BpComplex,
    ) {
        complex.components?.forEach { has ->
            val component = physicalEntity(has.element)
            bpComplex.addComponentStoichiometry(
                model.addNew(Stoichiometry::class.java, context.nextId("Stoichiometry")).apply {
                    stoichiometricCoefficient = has.stoichiometry.toFloat()
                    physicalEntity = component
                },
            )
            bpComplex.addComponent(component)
        }
    }

    /**
     * A reference entity, once per file: its class, organism (a sequence's), names
     * (display name, names, gene names, each once), its own UnificationXref and its
     * first comment or, failing that, its first description.
     */
    private fun entityReference(
        reference: ReferenceEntity,
        entity: RPhysicalEntity,
    ): EntityReference =
        context.once("reference:${reference.dbId}") {
            val type =
                when {
                    reference is ReferenceGeneProduct -> ProteinReference::class.java
                    reference is ReferenceMolecule || reference is ReferenceGroup -> SmallMoleculeReference::class.java
                    reference is ReferenceDNASequence -> DnaReference::class.java
                    reference is ReferenceRNASequence -> RnaReference::class.java
                    reference is ReferenceTherapeutic && entity is ProteinDrug -> ProteinReference::class.java
                    reference is ReferenceTherapeutic && entity is RNADrug -> RnaReference::class.java
                    reference is ReferenceTherapeutic && entity is ChemicalDrug -> SmallMoleculeReference::class.java
                    else -> EntityReference::class.java
                }
            model.addNew(type, context.nextId(type.simpleName)).apply {
                if (this is SequenceEntityReference && reference is ReferenceSequence) {
                    reference.species?.let { organism = xrefs.bioSource(it) }
                }
                val names = LinkedHashSet<String>()
                reference.displayName?.let { names.add(it) }
                reference.name?.let { names.addAll(it) }
                if (reference is ReferenceSequence) reference.geneName?.let { names.addAll(it) }
                names.forEach { addName(it) }
                xrefs.referenceXref(reference)?.let { addXref(it) }
                if (reference is ReferenceSequence) {
                    (reference.comment?.firstOrNull() ?: reference.description?.firstOrNull())?.let { addComment(it) }
                }
            }
        }

    /** A ModificationFeature for each modified residue, once per residue. */
    private fun addModifications(
        entity: RPhysicalEntity,
        bpEntity: BpPhysicalEntity,
    ) {
        if (entity !is EntityWithAccessionedSequence) return
        entity.hasModifiedResidue?.forEach { bpEntity.addFeature(modificationFeature(it)) }
    }

    private fun modificationFeature(residue: AbstractModifiedResidue): EntityFeature =
        context.once("modification:${residue.dbId}") {
            model.addNew(ModificationFeature::class.java, context.nextId("ModificationFeature")).apply {
                when (residue) {
                    is TranslationalModification -> residue.coordinate
                    is ReplacedResidue -> residue.coordinate
                    is FragmentInsertionModification -> residue.coordinate
                    // A transcriptional one, not a translational one, though the converter read both alike
                    is ModifiedNucleotide -> residue.coordinate
                    else -> null
                }?.let { featureLocation = sequenceSite(it) }
                // The first PSI-MOD term, else what the residue is modified by.
                val psiMod =
                    when (residue) {
                        is TranslationalModification -> residue.psiMod
                        is ReplacedResidue -> residue.psiMod?.firstOrNull()
                        else -> null
                    }
                val modifiedBy =
                    when (residue) {
                        is GroupModifiedResidue -> residue.modification
                        is CrosslinkedResidue -> residue.modification
                        is ModifiedNucleotide -> residue.modification
                        else -> null
                    }
                when {
                    psiMod != null -> modificationType = psiModType(psiMod)
                    modifiedBy != null -> modificationType = modificationType(modifiedBy)
                }
                if (residue is FragmentModification) addComment(residue.displayName)
            }
        }

    // A PSI-MOD term as a vocabulary: its first name (else its display name) and its MOD: xref.
    private fun psiModType(psiMod: PsiMod): SequenceModificationVocabulary =
        context.once("modificationType:${psiMod.dbId}") {
            model.addNew(SequenceModificationVocabulary::class.java, context.nextId("SequenceModificationVocabulary")).apply {
                addTerm(psiMod.name?.firstOrNull() ?: psiMod.displayName)
                xrefs.referenceXref(psiMod)?.let { addXref(it) }
            }
        }

    /**
     * What a residue is modified by, when it has no PSI-MOD term: its display name, up
     * to any "[ChEBI", and the xref of its reference entity (its own, or a simple
     * entity's).
     */
    private fun modificationType(modifiedBy: DatabaseObject): SequenceModificationVocabulary =
        context.once("modificationType:${modifiedBy.dbId}") {
            model.addNew(SequenceModificationVocabulary::class.java, context.nextId("SequenceModificationVocabulary")).apply {
                val name = modifiedBy.displayName
                val at = name.indexOf("[ChEBI")
                addTerm(if (at > 0) name.substring(0, at).trim() else name)
                val reference =
                    when (modifiedBy) {
                        is ReferenceEntity -> modifiedBy
                        is SimpleEntity -> modifiedBy.referenceEntity
                        else -> null
                    }
                reference?.let { xrefs.referenceXref(it) }?.let { addXref(it) }
            }
        }

    /**
     * A FragmentFeature from a sequence's start and end coordinates, a new one each
     * time; none when both are the defaults (1, and -1 for the end).
     */
    private fun addFragment(
        entity: RPhysicalEntity,
        bpEntity: BpPhysicalEntity,
    ) {
        if (entity !is EntityWithAccessionedSequence) return
        val start = entity.startCoordinate ?: 1
        val end = entity.endCoordinate ?: -1
        if (start == 1 && end == -1) return
        bpEntity.addFeature(
            model.addNew(FragmentFeature::class.java, context.nextId("FragmentFeature")).apply {
                featureLocation =
                    model.addNew(SequenceInterval::class.java, context.nextId("SequenceInterval")).apply {
                        sequenceIntervalBegin = sequenceSite(start)
                        sequenceIntervalEnd = sequenceSite(end)
                    }
            },
        )
    }

    private fun sequenceSite(position: Int): SequenceSite =
        model.addNew(SequenceSite::class.java, context.nextId("SequenceSite")).apply {
            sequencePosition = position
            positionStatus = PositionStatusType.EQUAL
        }
}
