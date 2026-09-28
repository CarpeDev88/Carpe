# Carpe AI principles

Carpe's AI exists to help people evaluate whether technology is serving the lives they choose.

## Detect → Explain → Counter → Empower

1. **Detect** patterns only from information the user has explicitly allowed Carpe to access.
2. **Explain** observations and uncertainty in understandable language. Do not pretend correlation proves causation.
3. **Counter** unwanted patterns with proportionate, reversible friction and alternatives selected around the user's goals.
4. **Empower** the user to continue, reject, correct, or disable an intervention.

## Evaluation model

Carpe must not equate screen time with harm. Technology can support work, relationships, learning, health, creativity, navigation, accessibility, entertainment, and other goals.

Assessments should combine, where available:
- user-selected goals;
- explicit user feedback;
- intentionality;
- duration and timing patterns;
- voluntarily enabled device signals;
- intervention outcomes;
- uncertainty.

The user's explicit correction outranks an inferred classification.

## Outcome measures

Use voluntary, user-chosen check-ins to learn whether an action supported a goal. Show the time window and the evidence being summarized. A check-in is a private planning aid, not a score of the user or a measure of moral success.

Do not optimize CARPE's own opens, session length, retention, notifications, or streaks. Do not infer that less device use is always better. A successful intervention may be followed by the user closing CARPE, continuing to use another app deliberately, or choosing a different action.

For product evaluation, ask whether the user felt more in control and whether their chosen goal moved forward. Collect research feedback only through explicit opt-in, keep it separate from app operation, and allow withdrawal and deletion. Never turn any one proxy measure into an engagement target.

## Safety and autonomy constraints

- Never optimize for advertising, purchases, retention, daily active use, or time spent in Carpe.
- Never sell or monetize behavioral profiles.
- Never silently expand permissions or data collection.
- Keep core functionality useful without optional permissions.
- Prefer on-device inference when practical.
- Minimize retained data and make its purpose understandable.
- Show uncertainty when evidence is weak.
- Avoid moralizing ordinary technology use.
- Never infer sensitive traits merely to improve engagement.
- Recommendations should be explainable, dismissible, and reversible.


## Trust invariants

These are product constraints, not optimization targets:

- CARPE has no advertising or behavioral-data sales.
- Raw usage events and notification contents do not leave the device.
- Notification intelligence records metadata only; message content is not parsed or retained.
- Cloud AI receives data only through the explicit `CloudAiContext` allowlist.
- Core conversation and local actions remain useful without optional monitoring permissions.
- User goals outrank inferred goals. CARPE suggests; the user decides.
- No diagnosis of addiction or other conditions from device behavior.
- No covert or remote monitoring of another person.
- Recommendations should have a finite end state; CARPE should be comfortable being closed.
- Learned profile information must be visible and clearable by the user.
- Changes to these invariants require explicit review, documentation, and tests.
