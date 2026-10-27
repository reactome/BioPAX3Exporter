# Differences from the release's BioPAX

Every way this exporter's output differs from the release's published `biopax.zip`, with its count and its reason (spec 001, FR-002). A difference may stay for one of three reasons only: the order and ids; the data, with its ticket; or an obvious bug of the old converter, fixed here. The comparison is by meaning (`ci/compare.py`, research R2).

## The validator's baseline

The BioPAX validator 6.0.0 (`--profile=notstrict`), read by `ci/validate.py`, on the published files of release 97. A written file may have no ERROR its published counterpart lacks (SC-003); warnings are recorded, not held.

| Species | Errors | Warnings | Codes |
| --- | --- | --- | --- |
| _Plasmodium falciparum_ | 0 | 16,863 | 19 |

Measured 27 Oct 2026. A run takes about four minutes by the shell's clock (249 s with the network off, 242 s with it on), most of it the validator's start-up. The other species' baselines are made in T020.
