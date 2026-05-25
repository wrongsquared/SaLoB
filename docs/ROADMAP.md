# Roadmap

## ✅ Done

### OneMap Integration (Search Augmentation)
Backend OneMap client + service + controller (search combined, dedup by name, geocode on create). Frontend autocomplete in StepEatery and homepage SearchBar. Flyway migrations, DB trigger, EateryTypeController, API spec.

---

## Bugs

### mapStore crash — "Maximum update depth exceeded"
The frontend occasionally crashes with a React error about repeated `setState` calls originating from `mapStore`. Likely a circular update loop — map events → store update → re-render → map events. Needs investigation and fix.

---

## AI Assistant (Natural Language Query)

### Grill-Me Q&A (2026-05-24)

**Q1: Where does the LLM call live?**
- **Decision:** Backend, inside food-service. A new microservice is overkill for an MVP — adds Gradle module, Docker container, K8s config, CI pipeline for ~100 lines of logic. Extract later if needed.

**Q2: Python vs Java?**
- **Decision:** Java with Spring AI. Avoids introducing a new runtime (Python venv, pip, WSGI). Spring AI provides `BeanOutputConverter<T>` for structured JSON→record mapping, `@Tool` function calling, retry, token tracking — all within the existing Gradle build.

**Q3: LangChain/LangGraph?**
- **Decision:** Overkill for MVP. Single-turn pattern (user message → structured action → execute → respond). Direct OpenRouter call via Spring AI `ChatClient` is simpler and has zero framework lock-in. Add LangChain later if RAG/multi-agent needed.

**Q4: Structured output schema?**
- **Decision:** Sealed interface discriminated by `action` field:
  ```java
  public sealed interface ChatResponse permits NavigateAction, QueryPriceAction, ChatMessage {}
  public record NavigateAction(String route, UUID entityId, String label) implements ChatResponse {}
  public record QueryPriceAction(String foodName, String eateryName) implements ChatResponse {}
  public record ChatMessage(String text) implements ChatResponse {}
  ```

**Q5: API shape?**
- **Decision:** `POST /api/chat` with `{ messages: [{ role, content }] }`. Response includes `{ action, data, message }`. The frontend interprets the `action` field:
  | Action | Frontend behavior |
  |---|---|
  | `navigate` | `navigate(route)` to entity page |
  | `query_price` | Display price inline in chat bubble |
  | `error` | Show error text |
  | `clarify` | Show disambiguation prompt with clickable options |

**Q6: Conversation history?**
- **Decision:** Persist in a `chat_sessions` / `chat_messages` table (not just frontend-owned). Backend reads last N messages from DB per session.

**Q7: Name→ID resolution?**
- **Decision:** Backend handles it (not the LLM). LLM outputs food/eatery names, backend chains `searchEatery → getEateryDetail → filter foodPreviews` to resolve names to IDs. Keeps the LLM dumb and the backend reliable.

**Q8: Disambiguation?**
- **Decision:** If multiple eateries match, return `{ action: "clarify", options: [...] }`. Frontend shows clickable buttons in the chat.

**Q9: Chat UI?**
- **Decision:** Floating widget (Intercom-style) in `<LeLayout>`, collapsed by default, slides up on click. No new route needed.

**Q10: Rate limiting?**
- **Decision:** Reuse API gateway rate limiting. `/api/chat` should have tighter per-user limits (costs real money per call).

**Q11: OpenRouter key?**
- **Decision:** `OPENROUTER_API_KEY` in service `.env`. Spring AI OpenAI-compatible config maps it via `spring.ai.openai.api-key=${OPENROUTER_API_KEY}`.

**Q12: Model?**
- **Decision:** `deepseek-chat` (free tier on OpenRouter). Test structured output compliance early — fall back to lenient `BeanOutputConverter` if malformed JSON is frequent.

**Q13: Streaming?**
- **Decision:** Yes for chat messages (aesthetic). Structured actions (navigate, query_price) are still dispatched as single JSON — only the `message` text field streams.

**Q14: System prompt?**
- **Decision:** Embedded as Spring `@Value` resource (`prompts/chat-system.st`). Lists available actions, JSON schema, examples. Version-controlled.

**Q15: Testing?**
- **Decision:** Mock LLM return (canned JSON), test action execution logic end-to-end. Integration tests with `@SpringBootTest` + mocked `RestClient`.

**Q16: Observability?**
- **Decision:** Log last 200 chars of input + full LLM response + resolved action + latency. Standard SLF4J.

---

## Touch Ups

### Map Icon Visibility
Icons are hard to see against noisy OSM tiles (especially the small 14px Lucide SVGs on eatery markers).
**Open questions:**
- Darken or blur the tile layer behind markers? (CSS `mix-blend-mode` or a semi-transparent overlay)
- Increase circle size and icon scale on hover/selected state?
- Add a subtle white stroke/shadow around the icon SVG to separate it from the background?
- Possibly reduce tile opacity globally and render a flat coloured underlay?

### Map Clustering Aesthetics
Cluster numbers are barely readable — small text on a tiny circle.
**Open questions:**
- Replace the default spider/cluster with a larger circle (e.g. 48px diameter, bold white number on dark background)?
- Show a stacked row of miniature "pins" instead of a number badge (like Google Maps clusters)?
- Customize the `MarkerClusterGroup` iconCreateFunction to render a larger circle with the count?
- Should clusters show a sum of icons (e.g. 3 small Store icons) instead of a numeric count?

---

## Eatery Closure Flow
**Description:** Two-part feature — (1) "Report as Closed" button (frontend) → `POST /api/eateries/{id}/report-closed` (already exists ✅), (2) admin review flow that aggregates closure flags, auto-closes eatery after threshold, or flags for admin review.
**Dependencies:** Admin RBAC, notification system
**Key files:** `EateryClosureFlagRepository.java`, `EateryService.reportClosed()`, admin dashboard page

## AI Food Verification
**Description:** When a user creates a new food type (not in seeded database), automatically run through AI legitimacy check. AI approves/rejects based on plausibility (e.g., "Chicken Rice at $500" → reject, "Hainanese Chicken Rice at a McDonald's" → suspicious). No human moderation — AI decision is final. No corrections suggested.
**Dependencies:** LLM provider integration (OpenAI/Anthropic), prompt engineering for food legitimacy
**Key files:** `FoodService.java` (currently has TODO for AI pipeline), `FoodController.java`

## Performance Testing

**Stack:** k6 (JS/TypeScript) → JSON output, optionally → Prometheus + Grafana

**High-volume endpoints to load-test:**
| Endpoint | Trigger | Priority |
|---|---|---|
| `GET /api/eateries/within-bounds` | Map pan/zoom (every viewport change) | 🔴 Critical |
| `GET /api/food-entries/within-bounds` | Map pan/zoom in food mode | 🔴 Critical |
| `GET /api/eateries/search` | Search bar keystroke (debounced 300ms) | 🟡 High |
| `GET /api/foods/search` | Food tag picker keystroke | 🟡 High |
| `GET /api/eateries/{id}` | Marker click → sidebar detail | 🟡 High |
| `GET /api/food-entries/historical-data/{id}` | Detail page load | 🟢 Medium |
| `POST /api/food-entries/{id}/vote` | Vote button click | 🟢 Medium |
| `POST /api/food-entries/submit` | Price submission | 🟢 Medium |

**Monitoring:**
- **Tier 1 (immediate):** k6 `--out json=results.json` → manual inspection + `k6 report` HTML
- **Tier 2 (observability stack):** Add Prometheus + Grafana to `docker-compose.yaml`, k6 `--out experimental-prometheus-rw`, pre-built dashboards
- **k6 checks:** p95 latency < 500ms, error rate < 1%, no request timeouts

**Setup needed:**
- `backend/k6/` directory for test scripts
- `POST /api/auth/login` or direct token injection for authenticated endpoints (vote, submit)
- Prometheus + Grafana services in `docker-compose.yaml` (optional, Tier 2)

## Settings Page
**Description:** User profile editing, preferences, notification settings. Deferred from MVP.
**Dependencies:** Auth flow completion, user profile endpoints
