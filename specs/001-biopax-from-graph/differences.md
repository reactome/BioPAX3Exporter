# Differences from the release's BioPAX

Every way this exporter's output differs from the release's published `biopax.zip`, with its count and its reason (spec 001, FR-002). A difference may stay for one of three reasons only: the order and ids; the data, with its ticket; or an obvious bug of the old converter, fixed here. The comparison is by meaning (`ci/compare.py`, research R2).

## Found while building the comparer

- **`conversionDirection`.** Paxtools 6.0.0 writes `LEFT_TO_RIGHT` where release 97 has `LEFT-TO-RIGHT`. In _P. falciparum_ that is 966 triples on each side, one per BiochemicalReaction, measured by reading the published file with Paxtools 6.0.0 and writing it back. Which form the exporter writes, and whether the validator accepts both, is decided in T012.
- **What a Paxtools round trip changes.** Reading the published _P. falciparum_ file with Paxtools 6.0.0 and writing it back gives 4,176 triples only in the published file and 4,163 only in the copy. At the root (`ci/compare.py`) there are 2,639; the rest is carried by rehashed keys. The root changes:
  - the direction above, 966 a side;
  - 334 Evidence comments, which #17's review traced to Paxtools' reader, not its writer;
  - names: displayName (9 on the published side, 7 on the copy's), name (BiochemicalReaction 4 a side, SmallMoleculeReference 1 a side), 8 Protein and 3 SmallMolecule names on the published side only;
  - one PublicationXref title, 1 a side.

  What the reader does to names is not yet traced. A file written from the graph is compared in T020.
- **Values repeated within one individual.** Release 97 writes some values twice on one individual: 13 in _P. falciparum_ and 2,421 in human, 2,292 of them a `Protein.feature` written twice. The comparer counts a repeat once, since Paxtools keeps values as sets. Two individuals of the same content, referred to twice, still count twice; human has 15 such references. Whether a protein's repeated modification should be one feature or two is decided in T011.

## The validator's baseline

The BioPAX validator 6.0.0 (`--profile=notstrict`), read by `ci/validate.py`, on the published files of release 97. A written file may have no ERROR its published counterpart lacks (SC-003); warnings are recorded, not held.

| Species | Errors | Warnings | Codes |
| --- | --- | --- | --- |
| _Plasmodium falciparum_ | 0 | 16,863 | 19 |

Measured 27 Oct 2026. A run takes about four minutes by the shell's clock (249 s with the network off, 242 s with it on), most of it the validator's start-up. The other species' baselines are made in T020.
