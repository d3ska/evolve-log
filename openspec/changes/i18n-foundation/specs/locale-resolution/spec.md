# Spec: locale-resolution

### Requirement: Locale is resolved from user preference on every request

#### Scenario: Authenticated request
- **WHEN** any API endpoint is called by an authenticated user
- **THEN** the locale used for translation is `users.locale` for that user

#### Scenario: Unauthenticated request with Accept-Language header
- **WHEN** an unauthenticated request includes `Accept-Language: pl`
- **THEN** the locale resolves to `'pl'`

#### Scenario: No locale signal available
- **WHEN** no user preference and no Accept-Language header are present
- **THEN** locale defaults to `'en'`

### Requirement: Locale resolution does not affect non-translatable fields

#### Scenario: Numeric and date fields unaffected
- **WHEN** locale is set to `'pl'`
- **THEN** all numeric values (weights, reps, volume) and dates are returned in the same format as for `'en'` — locale affects only translatable string fields
