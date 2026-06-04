# Live Voting Updates (STOMP / WebSockets)

## Goal
Provide real-time vote updates so that when one client upvotes, downvotes, or removes a vote on a food entry, other connected clients immediately see the updated state.

This feature is server -> client only. Clients do not need to send messages over the WebSocket channel.

---

## High-level design

### Transport
Use WebSockets with STOMP.

### Ownership
The websocket endpoint lives in `food-service`, because food-service owns vote state and the vote update lifecycle.

### Gateway
The websocket connection is routed through `api-gateway`.

The gateway should:
- accept the initial WebSocket upgrade request
- validate JWT during the handshake using the same JWK/JWKS setup already used for REST traffic
- forward the connection to `food-service`

### Broker
Use RabbitMQ as the shared broker for STOMP messaging.

This is important because it allows:
- multiple food-service instances
- reliable fanout across instances
- a future Kubernetes deployment without sticky-session dependence

---

## Topic model

Start simple with subscriptions keyed by `foodEntryId`.

Suggested destination:
- `/topic/votes/food-entry/{foodEntryId}`

The payload should contain absolute state, not deltas.

Example:
```json
{
  "foodEntryId": "uuid",
  "upvotes": 12,
  "downvotes": 3,
  "confidence": 84.2,
  "version": 57,
  "updatedAt": "2026-06-01T10:15:30Z"
}
```

Why absolute values:
- avoids drift if a client misses one event
- makes reconnect/replay safer
- simplifies frontend state replacement

---

## Vote update flow

1. Client sends vote change via existing REST endpoint.
2. Food-service updates the database inside a transaction.
3. The service writes an outbox record in the same transaction.
4. A background publisher reads outbox rows and publishes a vote update message to RabbitMQ.
5. The STOMP broker relay forwards the message to all subscribed clients.
6. Clients update the visible vote counts in place.

---

## Reliability strategy: Outbox pattern

Use the outbox pattern so vote changes and vote update events cannot get out of sync.

### Why
If the DB commit succeeds but message publication fails, clients never see the update.

### Outbox flow
- write vote mutation
- write outbox row in same transaction
- separate publisher process reads unpublished rows
- publish to RabbitMQ
- mark row as published

This gives at-least-once publication with recoverability.

### Expected consequence
Duplicate delivery is possible, so clients should:
- replace state using absolute counts
- ignore stale updates using `version`

---

## Throttling / event coalescing

Vote spam can cause bursty updates.

Recommended approach:
- coalesce updates per `foodEntryId`
- emit no more than once every short window, e.g. 100–250 ms
- always send the latest absolute count

This reduces noisy UI churn during high-traffic voting.

---

## Authentication

At minimum:
- authenticate the websocket handshake at the gateway
- carry the resolved user identity into the downstream connection

For now, the messages themselves do not need user-specific payloads.

If user-specific vote views are added later, the same authenticated identity can be used to enrich payloads.

---

## Frontend behavior

Frontend should:
- subscribe only to food entries currently visible to the user
- apply optimistic UI updates immediately on vote click
- reconcile with server updates when the websocket event arrives
- debounce local UI refreshes if vote storms are expected

The websocket update should be treated as the source of truth.

---

## Scope for MVP

MVP scope:
- subscriptions by `foodEntryId`
- absolute vote counts
- authenticated websocket handshake through gateway
- RabbitMQ-backed STOMP relay
- outbox-based event publication

Deferred:
- eatery-level subscriptions
- map-bounds subscriptions
- user-specific websocket queues
- replay/history of missed events
- administrative moderation channels

---

## Risks / hitches

### Multiple service instances
In-memory websocket broadcasting is not sufficient. Use RabbitMQ STOMP relay.

### Message ordering
Ordering is best-effort. Use absolute state and version numbers.

### Missing events
Clients may miss an event on reconnect. Absolute payloads and versioning keep the UI consistent.

### Gateway timeout / websocket upgrade config
WebSocket routes need separate gateway configuration from normal REST requests.

### Update storms
Throttle or coalesce vote updates before broadcasting.

### Auth model mismatch
If gateway and food-service disagree on websocket authentication, connections may fail or become inconsistent.

---

## Implementation plan

1. Add websocket/STOMP dependencies to `food-service`.
2. Enable STOMP broker relay to RabbitMQ.
3. Add an authenticated websocket handshake path via the gateway.
4. Publish vote update events from the vote write path via outbox.
5. Add a STOMP endpoint for clients to subscribe by `foodEntryId`.
6. Update frontend to subscribe/unsubscribe based on visible entries.
7. Add tests for:
   - vote mutation
   - outbox publication
   - STOMP message fanout
   - reconnect behavior
