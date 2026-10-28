package org.reactome.server.tools

import org.biopax.paxtools.model.level3.BioSource
import org.biopax.paxtools.model.level3.Complex
import org.biopax.paxtools.model.level3.FragmentFeature
import org.biopax.paxtools.model.level3.ModificationFeature
import org.biopax.paxtools.model.level3.PhysicalEntity
import org.biopax.paxtools.model.level3.PositionStatusType
import org.biopax.paxtools.model.level3.Protein
import org.biopax.paxtools.model.level3.ProteinReference
import org.biopax.paxtools.model.level3.RelationshipXref
import org.biopax.paxtools.model.level3.SequenceInterval
import org.biopax.paxtools.model.level3.SequenceSite
import org.biopax.paxtools.model.level3.SmallMolecule
import org.biopax.paxtools.model.level3.SmallMoleculeReference
import org.biopax.paxtools.model.level3.UnificationXref
import org.biopax.paxtools.model.level3.Xref
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.ChemicalDrug
import org.reactome.server.graph.domain.model.Compartment
import org.reactome.server.graph.domain.model.DatabaseIdentifier
import org.reactome.server.graph.domain.model.DefinedSet
import org.reactome.server.graph.domain.model.Disease
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence
import org.reactome.server.graph.domain.model.FragmentReplacedModification
import org.reactome.server.graph.domain.model.GenomeEncodedEntity
import org.reactome.server.graph.domain.model.GroupModifiedResidue
import org.reactome.server.graph.domain.model.ModifiedNucleotide
import org.reactome.server.graph.domain.model.ModifiedResidue
import org.reactome.server.graph.domain.model.ProteinDrug
import org.reactome.server.graph.domain.model.PsiMod
import org.reactome.server.graph.domain.model.ReferenceGeneProduct
import org.reactome.server.graph.domain.model.ReferenceGroup
import org.reactome.server.graph.domain.model.ReferenceIsoform
import org.reactome.server.graph.domain.model.ReferenceMolecule
import org.reactome.server.graph.domain.model.ReferenceTherapeutic
import org.reactome.server.graph.domain.model.ReplacedResidue
import org.reactome.server.graph.domain.model.SimpleEntity
import org.reactome.server.graph.domain.model.Species
import org.reactome.server.graph.domain.relationship.HasComponent
import java.util.TreeSet
import org.reactome.server.graph.domain.model.Complex as RComplex

/**
 * Physical entities as release 97's converter writes them (spec 001 T011), on a real
 * Paxtools model. Graph-core's objects are mocks, as in the other tests: each is made
 * before the next is stubbed, since Mockito refuses one made mid-stubbing.
 */
class BioPAXPhysicalEntityBuilderTest {
    private val context = ConversionContext("http://www.reactome.org/biopax/97/1#", 97)
    private val builder = BioPAXPhysicalEntityBuilder(context)

    private fun species(): Species {
        val taxonomy = mock(DatabaseIdentifier::class.java)
        whenever(taxonomy.dbId).thenReturn(9606001)
        whenever(taxonomy.databaseName).thenReturn("NCBI Taxonomy")
        whenever(taxonomy.identifier).thenReturn("9606")
        return mock(Species::class.java).also {
            whenever(it.dbId).thenReturn(48887)
            whenever(it.displayName).thenReturn("Homo sapiens")
            whenever(it.crossReference).thenReturn(listOf(taxonomy))
        }
    }

    private fun compartment(): Compartment =
        mock(Compartment::class.java).also {
            whenever(it.dbId).thenReturn(70101)
            whenever(it.name).thenReturn("cytosol")
            whenever(it.accession).thenReturn("0005829")
        }

    private fun geneProduct(
        dbId: Long = 9000,
        isoform: Boolean = false,
    ): ReferenceGeneProduct {
        val species = species()
        val reference = if (isoform) mock(ReferenceIsoform::class.java) else mock(ReferenceGeneProduct::class.java)
        whenever(reference.dbId).thenReturn(dbId)
        whenever(reference.displayName).thenReturn("UniProt:P62877 RBX1")
        whenever(reference.name).thenReturn(listOf("RBX1", "UniProt:P62877 RBX1", "RNF75"))
        whenever(reference.geneName).thenReturn(listOf("RBX1", "ROC1"))
        whenever(reference.identifier).thenReturn("P62877")
        whenever(reference.databaseName).thenReturn("UniProt")
        whenever(reference.species).thenReturn(species)
        whenever(reference.comment).thenReturn(listOf("FUNCTION one", "FUNCTION two"))
        whenever(reference.description).thenReturn(listOf("a description"))
        if (reference is ReferenceIsoform) whenever(reference.variantIdentifier).thenReturn("P62877-2")
        return reference
    }

    private fun protein(
        dbId: Long = 100,
        reference: ReferenceGeneProduct = geneProduct(),
    ): EntityWithAccessionedSequence {
        val compartment = compartment()
        return mock(EntityWithAccessionedSequence::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.stId).thenReturn("R-HSA-$dbId")
            whenever(it.stIdVersion).thenReturn("R-HSA-$dbId.3")
            whenever(it.displayName).thenReturn("RBX1 [cytosol]")
            whenever(it.name).thenReturn(listOf("E3 ubiquitin-protein ligase RBX1", "RBX1"))
            whenever(it.compartment).thenReturn(listOf(compartment))
            whenever(it.referenceEntity).thenReturn(reference)
            // Unset, as the graph leaves them (Mockito answers 0 for an Integer)
            whenever(it.startCoordinate).thenReturn(null)
            whenever(it.endCoordinate).thenReturn(null)
        }
    }

    private fun smallMolecule(dbId: Long = 113592): SimpleEntity {
        val reference = mock(ReferenceMolecule::class.java)
        whenever(reference.dbId).thenReturn(dbId + 1)
        whenever(reference.displayName).thenReturn("ATP(4-) [ChEBI:30616]")
        whenever(reference.name).thenReturn(listOf("ATP(4-)", "ATP"))
        whenever(reference.identifier).thenReturn("30616")
        whenever(reference.databaseName).thenReturn("ChEBI")
        return mock(SimpleEntity::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("ATP [cytosol]")
            whenever(it.name).thenReturn(listOf("ATP"))
            whenever(it.referenceEntity).thenReturn(reference)
        }
    }

    private fun Xref.dbAndId() = "$db $id"

    private inline fun <reified T : Xref> PhysicalEntity.xrefsOf() = xref.filterIsInstance<T>().map { it.dbAndId() }.toSet()

    @Test
    fun `a protein, as the release writes it`() {
        val bp = builder.physicalEntity(protein()) as Protein

        // The shortest name is the display name, the others names.
        assertEquals("RBX1", bp.displayName)
        assertEquals(setOf("E3 ubiquitin-protein ligase RBX1"), bp.name - bp.displayName)
        assertEquals("cytosol", bp.cellularLocation.term.single())
        assertEquals(
            setOf("GENE ONTOLOGY GO:0005829"),
            bp.cellularLocation.xref
                .map { it.dbAndId() }
                .toSet(),
        )
        assertEquals(setOf("Reactome DB_ID: 100"), bp.comment)
        assertEquals(setOf("Reactome Database ID Release 97 100", "Reactome R-HSA-100"), bp.xrefsOf<UnificationXref>())
        val stableId = bp.xref.filterIsInstance<UnificationXref>().single { it.db == "Reactome" }
        assertEquals("3", stableId.idVersion)
        assertEquals(
            "Reactome stable identifier. Use this URL to connect to the web page of this instance in Reactome: " +
                "http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=R-HSA-100.3",
            stableId.comment.single(),
        )
        val databaseId = bp.xref.filterIsInstance<UnificationXref>().single { it.db.startsWith("Reactome Database") }
        assertEquals(
            "Database identifier. Use this URL to connect to the web page of this instance in Reactome: " +
                "http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=100",
            databaseId.comment.single(),
        )
        assertEquals(
            "Reactome",
            bp.dataSource
                .single()
                .name
                .single(),
        )

        val reference = bp.entityReference as ProteinReference
        // Display name, names and gene names, each once.
        assertEquals(setOf("UniProt:P62877 RBX1", "RBX1", "RNF75", "ROC1"), reference.name)
        assertEquals(setOf("UniProt P62877"), reference.xref.map { it.dbAndId() }.toSet())
        assertEquals(setOf("FUNCTION one"), reference.comment)
        val organism = reference.organism
        assertEquals(setOf("Homo sapiens"), organism.name)
        assertEquals(setOf("NCBI Taxonomy 9606"), organism.xref.map { it.dbAndId() }.toSet())
    }

    @Test
    fun `an isoform's reference is its variant under UniProt Isoform`() {
        val bp = builder.physicalEntity(protein(reference = geneProduct(isoform = true))) as Protein
        assertEquals(
            setOf("UniProt Isoform P62877-2"),
            bp.entityReference.xref
                .map { it.dbAndId() }
                .toSet(),
        )
    }

    @Test
    fun `a small molecule's reference has the ChEBI prefix and no organism`() {
        val bp = builder.physicalEntity(smallMolecule()) as SmallMolecule
        assertEquals("ATP", bp.displayName)
        val reference = bp.entityReference as SmallMoleculeReference
        assertEquals(setOf("ChEBI CHEBI:30616"), reference.xref.map { it.dbAndId() }.toSet())
        assertEquals(setOf("ATP(4-) [ChEBI:30616]", "ATP(4-)", "ATP"), reference.name)
        assertTrue(context.model.getObjects(BioSource::class.java).isEmpty())
    }

    @Test
    fun `each entity, reference, location and xref once per file`() {
        val reference = geneProduct()
        val one = protein(100, reference)
        val other = protein(101, reference)
        val bp = builder.physicalEntity(one) as Protein
        assertSame(bp, builder.physicalEntity(one))
        val bpOther = builder.physicalEntity(other) as Protein
        assertSame(bp.entityReference, bpOther.entityReference)
        assertSame(bp.cellularLocation, bpOther.cellularLocation)
        assertEquals(1, context.model.getObjects(BioSource::class.java).size)
        assertEquals(1, context.model.getObjects(ProteinReference::class.java).size)
    }

    @Test
    fun `a complex has each component once, with its count`() {
        val atp = smallMolecule()
        val rbx1 = protein()
        val hasAtp = mock(HasComponent::class.java)
        whenever(hasAtp.element).thenReturn(atp)
        whenever(hasAtp.stoichiometry).thenReturn(2)
        whenever(hasAtp.order).thenReturn(0)
        val hasRbx1 = mock(HasComponent::class.java)
        whenever(hasRbx1.element).thenReturn(rbx1)
        whenever(hasRbx1.stoichiometry).thenReturn(1)
        whenever(hasRbx1.order).thenReturn(1)
        val components = TreeSet<HasComponent>(compareBy<HasComponent> { it.order })
        components.add(hasAtp)
        components.add(hasRbx1)
        val complex = mock(RComplex::class.java)
        whenever(complex.dbId).thenReturn(300)
        whenever(complex.displayName).thenReturn("ATP:RBX1 [cytosol]")
        whenever(complex.components).thenReturn(components)

        val bp = builder.physicalEntity(complex) as Complex
        assertEquals(setOf("ATP", "RBX1"), bp.component.map { it.displayName }.toSet())
        assertEquals(
            mapOf("ATP" to 2f, "RBX1" to 1f),
            bp.componentStoichiometry.associate { it.physicalEntity.displayName to it.stoichiometricCoefficient },
        )
        assertSame(builder.physicalEntity(atp), bp.component.single { it.displayName == "ATP" })
    }

    @Test
    fun `its features - modifications and a fragment`() {
        val psiMod = mock(PsiMod::class.java)
        whenever(psiMod.dbId).thenReturn(400)
        whenever(psiMod.name).thenReturn(listOf("O-phospho-L-serine", "phosphoserine"))
        whenever(psiMod.displayName).thenReturn("O-phospho-L-serine [MOD:00046]")
        whenever(psiMod.identifier).thenReturn("00046")
        whenever(psiMod.databaseName).thenReturn("MOD")
        val phospho = mock(ModifiedResidue::class.java)
        whenever(phospho.dbId).thenReturn(401)
        whenever(phospho.coordinate).thenReturn(66)
        whenever(phospho.psiMod).thenReturn(psiMod)
        // No PSI-MOD term: the type is what the residue is modified by.
        val modifier = smallMolecule(500)
        val grouped = mock(GroupModifiedResidue::class.java)
        whenever(grouped.dbId).thenReturn(402)
        whenever(grouped.modification).thenReturn(modifier)
        whenever(grouped.coordinate).thenReturn(null)
        // Modified by a reference group: its name up to "[ChEBI", and its own xref.
        val ubiquitin = mock(ReferenceGroup::class.java)
        whenever(ubiquitin.dbId).thenReturn(510)
        whenever(ubiquitin.displayName).thenReturn("polyubiquitin [ChEBI:12345]")
        whenever(ubiquitin.identifier).thenReturn("12345")
        whenever(ubiquitin.databaseName).thenReturn("ChEBI")
        val ubiquitinated = mock(GroupModifiedResidue::class.java)
        whenever(ubiquitinated.dbId).thenReturn(404)
        whenever(ubiquitinated.modification).thenReturn(ubiquitin)
        whenever(ubiquitinated.coordinate).thenReturn(48)
        val fragment = mock(FragmentReplacedModification::class.java)
        whenever(fragment.dbId).thenReturn(403)
        whenever(fragment.displayName).thenReturn("Replaced residues 120-130")
        val entity = protein()
        whenever(entity.hasModifiedResidue).thenReturn(listOf(phospho, grouped, ubiquitinated, fragment))
        whenever(entity.startCoordinate).thenReturn(23)
        whenever(entity.endCoordinate).thenReturn(108)

        val bp = builder.physicalEntity(entity)
        val modifications = bp.feature.filterIsInstance<ModificationFeature>()
        val byPsiMod = modifications.single { it.modificationType?.term == setOf("O-phospho-L-serine") }
        assertEquals(66, (byPsiMod.featureLocation as SequenceSite).sequencePosition)
        assertEquals(PositionStatusType.EQUAL, (byPsiMod.featureLocation as SequenceSite).positionStatus)
        assertEquals(
            setOf("MOD MOD:00046"),
            byPsiMod.modificationType.xref
                .map { it.dbAndId() }
                .toSet(),
        )
        // "ATP [cytosol]" has no "[ChEBI" to cut; its reference's xref is the type's.
        val byModifier = modifications.single { it.modificationType?.term == setOf("ATP [cytosol]") }
        assertEquals(
            setOf("ChEBI CHEBI:30616"),
            byModifier.modificationType.xref
                .map { it.dbAndId() }
                .toSet(),
        )
        assertNull(byModifier.featureLocation)
        val byGroup = modifications.single { it.modificationType?.term == setOf("polyubiquitin") }
        assertEquals(
            setOf("ChEBI CHEBI:12345"),
            byGroup.modificationType.xref
                .map { it.dbAndId() }
                .toSet(),
        )
        assertEquals(48, (byGroup.featureLocation as SequenceSite).sequencePosition)
        assertEquals(setOf("Replaced residues 120-130"), modifications.single { it.modificationType == null }.comment)

        val interval =
            bp.feature
                .filterIsInstance<FragmentFeature>()
                .single()
                .featureLocation as SequenceInterval
        assertEquals(23, interval.sequenceIntervalBegin.sequencePosition)
        assertEquals(108, interval.sequenceIntervalEnd.sequencePosition)
    }

    @Test
    fun `no fragment for the default coordinates, and an unset end is -1`() {
        assertTrue(builder.physicalEntity(protein(100)).feature.isEmpty())
        val fromThree = protein(101)
        whenever(fromThree.startCoordinate).thenReturn(3)
        val interval =
            builder
                .physicalEntity(
                    fromThree,
                ).feature
                .filterIsInstance<FragmentFeature>()
                .single()
                .featureLocation as SequenceInterval
        assertEquals(3, interval.sequenceIntervalBegin.sequencePosition)
        assertEquals(-1, interval.sequenceIntervalEnd.sequencePosition)
    }

    @Test
    fun `every cross-reference and disease, though one was written before`() {
        val mondo = mock(DatabaseIdentifier::class.java)
        whenever(mondo.dbId).thenReturn(600)
        whenever(mondo.databaseName).thenReturn("Mondo")
        whenever(mondo.identifier).thenReturn("0004963")
        val cancer = mock(Disease::class.java)
        whenever(cancer.dbId).thenReturn(601)
        whenever(cancer.databaseName).thenReturn("DOID")
        whenever(cancer.identifier).thenReturn("162")
        val other = mock(Disease::class.java)
        whenever(other.dbId).thenReturn(602)
        whenever(other.databaseName).thenReturn("DOID")
        whenever(other.identifier).thenReturn("5603")
        val first = protein(100)
        whenever(first.disease).thenReturn(listOf(cancer))
        whenever(first.crossReference).thenReturn(listOf(mondo))
        val second = protein(101)
        // The release's converter stopped at the first it had written before.
        whenever(second.disease).thenReturn(listOf(cancer, other))
        whenever(second.crossReference).thenReturn(listOf(mondo))

        builder.physicalEntity(first)
        val bp = builder.physicalEntity(second)
        assertEquals(setOf("Mondo 0004963", "DOID 162", "DOID 5603"), bp.xrefsOf<RelationshipXref>())
        bp.xref.filterIsInstance<RelationshipXref>().forEach {
            assertEquals(setOf("additional information"), it.relationshipType.term)
            assertEquals(
                setOf("MI MI:0361"),
                it.relationshipType.xref
                    .map { x -> x.dbAndId() }
                    .toSet(),
            )
        }
    }

    @Test
    fun `a set is a PhysicalEntity, with no members yet (T017)`() {
        val set = mock(DefinedSet::class.java)
        whenever(set.dbId).thenReturn(700)
        whenever(set.displayName).thenReturn("RBX1, CUL1")
        val bp = builder.physicalEntity(set)
        assertEquals(PhysicalEntity::class.java, bp.modelInterface)
        assertEquals("RBX1, CUL1", bp.displayName)
        assertEquals(setOf("Reactome DB_ID: 700"), bp.comment)
    }

    @Test
    fun `an entity with no reference is a PhysicalEntity`() {
        val gee = mock(GenomeEncodedEntity::class.java)
        whenever(gee.dbId).thenReturn(800)
        whenever(gee.displayName).thenReturn("unknown protein")
        assertEquals(PhysicalEntity::class.java, builder.physicalEntity(gee).modelInterface)
    }

    @Test
    fun `a chemical drug is a SmallMolecule, said to be converted from a drug`() {
        val therapeutic = mock(ReferenceTherapeutic::class.java)
        whenever(therapeutic.dbId).thenReturn(901)
        whenever(therapeutic.displayName).thenReturn("Guide to Pharmacology - Ligands:5687 erlotinib")
        whenever(therapeutic.identifier).thenReturn("4920")
        whenever(therapeutic.databaseName).thenReturn("Guide to Pharmacology - Ligands")
        val drug = mock(ChemicalDrug::class.java)
        whenever(drug.dbId).thenReturn(900)
        whenever(drug.displayName).thenReturn("erlotinib [cytosol]")
        whenever(drug.referenceEntity).thenReturn(therapeutic)
        val bp = builder.physicalEntity(drug) as SmallMolecule
        assertEquals(setOf("Converted from Drug in Reactome", "Reactome DB_ID: 900"), bp.comment)
        assertEquals(SmallMoleculeReference::class.java, bp.entityReference.modelInterface)
    }

    @Test
    fun `a modified nucleotide - its coordinate and what it is modified by`() {
        val polyA = mock(ReferenceGroup::class.java)
        whenever(polyA.dbId).thenReturn(1000)
        whenever(polyA.displayName).thenReturn("polyadenosine monophosphate [ChEBI:16000]")
        whenever(polyA.identifier).thenReturn("16000")
        whenever(polyA.databaseName).thenReturn("ChEBI")
        val residue = mock(ModifiedNucleotide::class.java)
        whenever(residue.dbId).thenReturn(1001)
        whenever(residue.coordinate).thenReturn(1141)
        whenever(residue.modification).thenReturn(polyA)
        val entity = protein()
        whenever(entity.hasModifiedResidue).thenReturn(listOf(residue))

        val feature =
            builder
                .physicalEntity(entity)
                .feature
                .filterIsInstance<ModificationFeature>()
                .single()
        assertEquals(1141, (feature.featureLocation as SequenceSite).sequencePosition)
        assertEquals(setOf("polyadenosine monophosphate"), feature.modificationType.term)
        assertEquals(
            setOf("ChEBI CHEBI:16000"),
            feature.modificationType.xref
                .map { it.dbAndId() }
                .toSet(),
        )
    }

    @Test
    fun `a replaced residue - its coordinate and its first PSI-MOD term`() {
        val removal = mock(PsiMod::class.java)
        whenever(removal.dbId).thenReturn(1100)
        whenever(removal.name).thenReturn(listOf("L-leucine removal"))
        whenever(removal.identifier).thenReturn("01641")
        whenever(removal.databaseName).thenReturn("MOD")
        val proline = mock(PsiMod::class.java)
        whenever(proline.dbId).thenReturn(1101)
        whenever(proline.name).thenReturn(listOf("L-proline residue"))
        val replaced = mock(ReplacedResidue::class.java)
        whenever(replaced.dbId).thenReturn(1102)
        whenever(replaced.coordinate).thenReturn(254)
        whenever(replaced.psiMod).thenReturn(listOf(removal, proline))
        val entity = protein()
        whenever(entity.hasModifiedResidue).thenReturn(listOf(replaced))

        val feature =
            builder
                .physicalEntity(entity)
                .feature
                .filterIsInstance<ModificationFeature>()
                .single()
        assertEquals(254, (feature.featureLocation as SequenceSite).sequencePosition)
        assertEquals(setOf("L-leucine removal"), feature.modificationType.term)
        assertEquals(
            setOf("MOD MOD:01641"),
            feature.modificationType.xref
                .map { it.dbAndId() }
                .toSet(),
        )
    }

    @Test
    fun `a protein drug is a Protein, its reference a ProteinReference`() {
        val therapeutic = mock(ReferenceTherapeutic::class.java)
        whenever(therapeutic.dbId).thenReturn(1201)
        whenever(therapeutic.displayName).thenReturn("Guide to Pharmacology - Ligands:6780 trastuzumab")
        whenever(therapeutic.identifier).thenReturn("6780")
        whenever(therapeutic.databaseName).thenReturn("Guide to Pharmacology - Ligands")
        val drug = mock(ProteinDrug::class.java)
        whenever(drug.dbId).thenReturn(1200)
        whenever(drug.displayName).thenReturn("trastuzumab [extracellular region]")
        whenever(drug.referenceEntity).thenReturn(therapeutic)
        val bp = builder.physicalEntity(drug) as Protein
        assertEquals(ProteinReference::class.java, bp.entityReference.modelInterface)
        assertEquals(
            setOf("Guide to Pharmacology - Ligands 6780"),
            bp.entityReference.xref
                .map { it.dbAndId() }
                .toSet(),
        )
    }

    @Test
    fun `a reference without a comment has its first description`() {
        val reference = geneProduct()
        whenever(reference.comment).thenReturn(null)
        whenever(reference.description).thenReturn(listOf("a description", "another description"))
        val bp = builder.physicalEntity(protein(reference = reference)) as Protein
        assertEquals(setOf("a description"), bp.entityReference.comment)
    }

    @Test
    fun `a PSI-MOD term wins over what the residue is modified by`() {
        val psiMod = mock(PsiMod::class.java)
        whenever(psiMod.dbId).thenReturn(1300)
        whenever(psiMod.name).thenReturn(listOf("ubiquitinylated lysine"))
        val chain = smallMolecule(1302)
        val residue = mock(GroupModifiedResidue::class.java)
        whenever(residue.dbId).thenReturn(1301)
        whenever(residue.psiMod).thenReturn(psiMod)
        whenever(residue.modification).thenReturn(chain)
        whenever(residue.coordinate).thenReturn(null)
        val entity = protein()
        whenever(entity.hasModifiedResidue).thenReturn(listOf(residue))

        val feature =
            builder
                .physicalEntity(entity)
                .feature
                .filterIsInstance<ModificationFeature>()
                .single()
        assertEquals(setOf("ubiquitinylated lysine"), feature.modificationType.term)
    }
}
