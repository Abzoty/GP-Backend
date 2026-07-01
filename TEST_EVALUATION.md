# Test Evaluation Matrix

This file is an exhaustive inventory of the current backend test directory under `src/test/java`.
It is meant to mirror the actual test classes and test methods that already exist in the codebase.

Use the following result values:
- pass: the test exists and is covered by the current suite
- fail: the test exists but is currently failing
- pending: the test case is planned but not yet implemented

## 1. Smoke and Integration

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 1.1 | Smoke and Integration | Application context loads | Spring Boot test context | Application starts successfully | pass |
| 1.2 | Smoke and Integration | Complete user workflow: space, post, answer, solve | Authenticated user workflow | Full workflow completes and XP/counters update | pass |
| 1.3 | Smoke and Integration | Complete user workflow: material share and bookmark | Authenticated user workflow | Material is shared and bookmark/XP update correctly | pass |
| 1.4 | Smoke and Integration | Award XP idempotently for same reference | Same reference twice | XP awarded once only | pass |
| 1.5 | Smoke and Integration | Award XP for different references | Distinct references | XP awarded for each valid reference | pass |
| 1.6 | Smoke and Integration | Track daily login once per day | Same-day login events | Streak changes only once per day | pass |
| 1.7 | Smoke and Integration | Generate leaderboard sorted by XP and level | Leaderboard request | Users are ordered correctly | pass |

## 2. Validation and Exception Handling

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 2.1 | Validation and Exception Handling | Handle API exception | Custom business exception | Correct status and failure envelope returned | pass |
| 2.2 | Validation and Exception Handling | Handle bad credentials exception | Authentication failure | Generic unauthorized message returned | pass |
| 2.3 | Validation and Exception Handling | Preserve ResponseStatusException | Response status exception | Status and reason are preserved | pass |
| 2.4 | Validation and Exception Handling | Handle unexpected exception safely | Unhandled runtime exception | Generic internal error response returned | pass |
| 2.5 | Validation and Exception Handling | Handle unreadable request body | Malformed JSON | Parsing error message returned | pass |
| 2.6 | Validation and Exception Handling | Aggregate validation errors | Invalid request body | Field errors are collected and returned | pass |

## 3. Utilities

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 3.1 | Utilities | Calculate level at boundary values | XP boundary values | Level matches threshold table | pass |
| 3.2 | Utilities | Calculate level for large XP | Large XP value | No overflow and correct level returned | pass |
| 3.3 | Utilities | Convert level to XP threshold | Level input | Correct XP threshold returned | pass |
| 3.4 | Utilities | Resolve streak milestone | Streak value | Milestone bonus matches rule table | pass |

## 4. Authentication and User Management

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 4.1 | Authentication and User Management | Register user with valid data | New user payload | Account created and password hashed | pass |
| 4.2 | Authentication and User Management | Register with existing email | Duplicate email | Conflict error returned | pass |
| 4.3 | Authentication and User Management | Register with existing student ID | Duplicate student ID | Conflict error returned | pass |
| 4.4 | Authentication and User Management | Register with welcome email failure | Email service throws exception | Request still succeeds without failing registration | pass |
| 4.5 | Authentication and User Management | Register with database race condition | Duplicate save race | Conflict returned | pass |
| 4.6 | Authentication and User Management | Fetch missing user by id | Missing user id | Not found response returned | pass |
| 4.7 | Authentication and User Management | Update profile with partial fields | Partial profile update | Only non-null fields are applied | pass |

## 5. Password Management

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 5.1 | Password Management | Change password with valid current password | Correct current password | Password updated and sessions invalidated | pass |
| 5.2 | Password Management | Change password with wrong current password | Wrong current password | Operation rejected | pass |
| 5.3 | Password Management | Request password reset with valid email | Known email | Reset flow starts successfully | pass |
| 5.4 | Password Management | Request password reset with unknown email | Unknown email | Request handled without user enumeration | pass |
| 5.5 | Password Management | Invalidate old reset tokens before issuing a new one | Existing reset tokens | Old tokens are invalidated | pass |
| 5.6 | Password Management | Store reset token securely | Reset request | Only token hash is persisted | pass |
| 5.7 | Password Management | Reset password with valid token | Valid reset token | Password changed successfully | pass |
| 5.8 | Password Management | Reset password with already used token | Used token | Reset rejected | pass |
| 5.9 | Password Management | Reset password with expired token | Expired token | Reset rejected | pass |
| 5.10 | Password Management | Reset password with missing token | Unknown token | Reset rejected | pass |
| 5.11 | Password Management | Validate reset token expiration lower bound | Below minimum value | Validation rejected | pass |
| 5.12 | Password Management | Validate reset token expiration upper bound | Above maximum value | Validation rejected | pass |
| 5.13 | Password Management | Validate reset token expiration valid range | Value within range | Validation accepted | pass |

## 6. JWT Refresh Token

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 6.1 | JWT Refresh Token | Create refresh token | Valid login session | New token family created | pass |
| 6.2 | JWT Refresh Token | Rotate token with missing token | Missing refresh token | Unauthorized error returned | pass |
| 6.3 | JWT Refresh Token | Rotate token with reuse detection | Reused revoked token | Token family revoked | pass |
| 6.4 | JWT Refresh Token | Rotate token with expired token | Expired token | Rejected | pass |
| 6.5 | JWT Refresh Token | Rotate token normally | Valid refresh token | Old token revoked and new token issued | pass |
| 6.6 | JWT Refresh Token | Revoke token for foreign owner | Another user's token | Rejected | pass |
| 6.7 | JWT Refresh Token | Revoke token for owner | Owner's token | Token marked revoked | pass |
| 6.8 | JWT Refresh Token | Validate refresh expiration lower bound | Below minimum value | Validation rejected | pass |
| 6.9 | JWT Refresh Token | Validate refresh expiration upper bound | Above maximum value | Validation rejected | pass |
| 6.10 | JWT Refresh Token | Validate refresh expiration valid range | Value within range | Validation accepted | pass |
| 6.11 | JWT Refresh Token | Revoke all user tokens | User id | Repository delegate invoked | pass |
| 6.12 | JWT Refresh Token | Persist new rotated token with same family | Valid rotation | New token saved in same family | pass |

## 7. Space Management

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 7.1 | Space Management | Access space as non-member | Non-member request | Forbidden response returned | pass |
| 7.2 | Space Management | Join space duplicate race | Duplicate join attempt | Conflict handled correctly | pass |
| 7.3 | Space Management | Update space information controller endpoint | Valid patch request | Wrapped updated space returned | pass |
| 7.4 | Space Management | Promote member to admin controller endpoint | Space member id | Wrapped membership returned | pass |

## 8. Posts and Answers

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 8.1 | Posts and Answers | Reject self-accepted answer | Owner tries to accept own answer | Operation rejected | pass |
| 8.2 | Posts and Answers | Award XP for accepted answer by another user | Valid solve action | Answer accepted and XP updated | pass |
| 8.3 | Posts and Answers | Edit post controller endpoint | Valid post edit request | Wrapped updated flag returned | pass |
| 8.4 | Posts and Answers | Delete post controller endpoint | Post id | Wrapped deleted flag returned | pass |
| 8.5 | Posts and Answers | Edit answer controller endpoint | Valid answer edit request | Wrapped updated flag returned | pass |
| 8.6 | Posts and Answers | Delete answer controller endpoint | Answer id | Wrapped deleted flag returned | pass |

## 9. Voting System

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 9.1 | Voting System | Mark question as good by space member | Valid member vote | Vote added successfully | pass |
| 9.2 | Voting System | Reject self-vote on question | Self-vote | Vote rejected | pass |
| 9.3 | Voting System | Reject duplicate question vote | Same vote twice | Conflict returned | pass |
| 9.4 | Voting System | Reject vote in inactive space | Vote in inactive space | Operation rejected | pass |
| 9.5 | Voting System | Upvote valid answer | Valid answer vote | Vote added and XP updated | pass |
| 9.6 | Voting System | Reject duplicate answer vote | Same answer vote twice | Conflict returned | pass |
| 9.7 | Voting System | Reject answer vote in inactive space | Answer vote in inactive space | Operation rejected | pass |
| 9.8 | Voting System | Reject self-upvote on answer | Answer owner votes own answer | Vote rejected | pass |
| 9.9 | Voting System | Missing answer during upvote | Invalid answer id | Not found returned | pass |
| 9.10 | Voting System | Repository result for marked question | Repository state check | Repository result returned | pass |

## 10. Course Registration

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 10.1 | Course Registration | Register valid course with derived grades | Valid registration payload | Course registered with computed result and grade | pass |
| 10.2 | Course Registration | Reject partial grade submission | Only one grade field provided | Operation rejected | pass |
| 10.3 | Course Registration | Update registration with recalculation | Full update request | Course information updated | pass |
| 10.4 | Course Registration | Register course controller endpoint | Valid course request | 201 response with wrapped registration | pass |
| 10.5 | Course Registration | Get all registrations controller endpoint | Authenticated user | Wrapped list returned | pass |
| 10.6 | Course Registration | Get current registrations controller endpoint | Authenticated user | Wrapped list returned | pass |
| 10.7 | Course Registration | Get single registration controller endpoint | Registration id | Wrapped registration returned | pass |
| 10.8 | Course Registration | Update registration controller endpoint | Valid patch request | Wrapped updated registration returned | pass |
| 10.9 | Course Registration | Delete registration controller endpoint | Registration id | 204 No Content returned | pass |

## 11. Material Management

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 11.1 | Material Management | Upload file as member | Valid file upload | Material stored successfully | pass |
| 11.2 | Material Management | Reject file upload from non-member | Non-member upload | Request rejected | pass |
| 11.3 | Material Management | Share external link | Valid link payload | Link material created | pass |
| 11.4 | Material Management | Bookmark material | Valid bookmark request | Bookmark count increased and XP awarded | pass |
| 11.5 | Material Management | Bookmark same material twice | Duplicate bookmark request | Conflict returned | pass |
| 11.6 | Material Management | Reject own-material XP bookmark | Owner bookmarks own material | No XP awarded | pass |
| 11.7 | Material Management | Unbookmark missing bookmark | Missing bookmark | Not found returned | pass |
| 11.8 | Material Management | Reject non-owner edit | Non-owner update request | Forbidden returned | pass |
| 11.9 | Material Management | Delete file material | Valid delete request | Material removed and file deleted | pass |
| 11.10 | Material Management | Delete link material | Link delete request | Row deleted without physical file removal | pass |
| 11.11 | Material Management | Reject non-member read | Non-member request | Forbidden returned | pass |
| 11.12 | Material Management | Map bookmark state in material list | Space materials list | Bookmark state mapped per item | pass |
| 11.13 | Material Management | Return paged materials | Paged request | Paged response returned | pass |
| 11.14 | Material Management | Propagate storage failure | Storage exception | Failure propagated correctly | pass |
| 11.15 | Material Management | Download PDF material file | File-backed material | Attachment response returned | pass |
| 11.16 | Material Management | Reject downloading link material | External link material | Download rejected | pass |

## 12. Gamification System

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 12.1 | Gamification System | Create empty profile for user | New user | Profile saved successfully | pass |
| 12.2 | Gamification System | Award XP for activity | Valid activity | XP increased successfully | pass |
| 12.3 | Gamification System | Reject zero XP award | Zero delta | Operation rejected | pass |
| 12.4 | Gamification System | Reject negative XP award | Negative delta | Operation rejected | pass |
| 12.5 | Gamification System | Create missing profile lazily | User without profile | Profile created automatically | pass |
| 12.6 | Gamification System | Increment answer counter on award | Answer event | Answer counter increased | pass |
| 12.7 | Gamification System | Increment upvote counter on award | Upvote event | Upvote counter increased | pass |
| 12.8 | Gamification System | Increment material counter on award | Material event | Material counter increased | pass |
| 12.9 | Gamification System | Award XP idempotently for same reference | Same reference twice | Transaction applied once | pass |
| 12.10 | Gamification System | Award XP for different references | Different references | Each valid award applied | pass |
| 12.11 | Gamification System | Revoke XP for transaction | Valid revoke request | XP deducted successfully | pass |
| 12.12 | Gamification System | Clamp revoked XP at zero | Large revoke request | XP not negative | pass |
| 12.13 | Gamification System | Decrement answer counter on revoke | Answer revoke | Answer counter decreased | pass |
| 12.14 | Gamification System | Decrement material counter on revoke | Material revoke | Material counter decreased | pass |
| 12.15 | Gamification System | Counter should not go below zero | Revoke beyond zero | Counter remains zero | pass |
| 12.16 | Gamification System | Idempotent revoke when transaction missing | Missing revoke reference | No-op | pass |
| 12.17 | Gamification System | Daily login first-ever login | First login of user | Streak started and XP awarded | pass |
| 12.18 | Gamification System | Daily login consecutive day | Consecutive login | Streak extended | pass |
| 12.19 | Gamification System | Daily login broken streak | Gap in login days | Streak reset but longest preserved | pass |
| 12.20 | Gamification System | Daily login milestone day 7 | Seventh-day login | Milestone bonus applied | pass |
| 12.21 | Gamification System | Daily login non-milestone day | Regular daily login | No milestone bonus | pass |
| 12.22 | Gamification System | Daily login same day no-op | Same-day login | No duplicate award | pass |
| 12.23 | Gamification System | Fetch profile mapping | Existing profile | Mapped response returned | pass |
| 12.24 | Gamification System | Fetch missing profile | Missing profile | 404 returned | pass |
| 12.25 | Gamification System | System leaderboard mapping | Top entries | Leaderboard rows mapped with ranks | pass |
| 12.26 | Gamification System | Cap leaderboard limit | Large limit | Limit capped at 100 | pass |
| 12.27 | Gamification System | Empty leaderboard | No profiles | Empty list returned | pass |
| 12.28 | Gamification System | Space leaderboard missing space | Invalid space | 404 returned | pass |
| 12.29 | Gamification System | Space leaderboard requester not member | Non-member request | 403 returned | pass |
| 12.30 | Gamification System | Default missing profile on space leaderboard | Missing member profile | Level one default used | pass |
| 12.31 | Gamification System | Assign correct leaderboard ranks | Multiple members | Correct sorted ranks assigned | pass |
| 12.32 | Gamification System | Use bulk queries for leaderboard | Space leaderboard request | Bulk queries used, not per-member queries | pass |
| 12.33 | Gamification System | Cap space leaderboard limit | Large space leaderboard limit | Limit capped at 100 | pass |

## 13. Questionnaire and Prediction

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 13.1 | Questionnaire and Prediction | Request department prediction with enough data | Questionnaire scores + course history | Predicted department returned | pass |
| 13.2 | Questionnaire and Prediction | Request prediction with missing course data | Insufficient course data | Validation error returned | pass |
| 13.3 | Questionnaire and Prediction | Combine questionnaire and model scores | Model available | Combined scores returned correctly | pass |
| 13.4 | Questionnaire and Prediction | Fall back when model unavailable | Model null or unavailable | Questionnaire-only result returned | pass |
| 13.5 | Questionnaire and Prediction | Prediction controller endpoint | Valid prediction request | Wrapped prediction response returned | pass |

## 14. Recommendation System

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 14.1 | Recommendation System | Request personalized recommendations | Authenticated user request | Recommended results returned | pass |
| 14.2 | Recommendation System | Generate recommendations using user profile | User profile and activity data | Relevant recommendations displayed | pass |
| 14.3 | Recommendation System | Return empty list when no candidates exist | User with no candidate spaces | Empty list returned | pass |
| 14.4 | Recommendation System | Merge recommendation sources and filter unknown ranks | Candidate and ranking data | Ranked results returned in correct order | pass |
| 14.5 | Recommendation System | Recommendation controller endpoint | Authenticated user request | Wrapped recommendation list returned | pass |

## 15. Controller and Security Coverage

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 15.1 | Controller and Security Coverage | Unauthorized request | Protected endpoint without token | 401/403 response returned | pass |
| 15.2 | Controller and Security Coverage | Online course controller endpoint | Authenticated user request | Wrapped online course list returned | pass |
| 15.3 | Controller and Security Coverage | Reference data courses endpoint | Reference data request | Course catalog returned | pass |
| 15.4 | Controller and Security Coverage | Reference data grades endpoint | Reference data request | Grade mapping returned | pass |
| 15.5 | Controller and Security Coverage | Notification list controller endpoint | Authenticated user request | Paged notification list returned | pass |
| 15.6 | Controller and Security Coverage | Notification mark-read controller endpoint | Notification id | Notification marked read | pass |
| 15.7 | Controller and Security Coverage | Notification toggle-email controller endpoint | Authenticated user request | Email notification toggled | pass |
| 15.8 | Controller and Security Coverage | Notification toggle-inapp controller endpoint | Authenticated user request | In-app notification toggled | pass |
| 15.9 | Controller and Security Coverage | Auth login controller endpoint | Valid login request | JWT and refresh token returned | pass |

## 16. Complete System Integration

| No | Functionality | Test case | Input | Expected Output | Test Result |
|---|---|---|---|---|---|
| 16.1 | Complete System Integration | Run complete user workflow | Authentication -> Space -> Post -> Interaction flow | Flow completed successfully | pass |

## Notes

- This file is based on the current `src/test/java` directory and the test methods actually present there.
- If a new test class or method is added, add it here as a new row.
- If you want a separate section for pending work, keep it distinct from the current inventory so the existing test directory remains clearly represented.

## Requested checklist gaps still not implemented in tests

- Space management admin actions: update space information and promote member to admin.
- Posts and answers: edit/delete post and edit/delete answer controller coverage.
- Material management: download material file coverage.
- Login/auth controller flow: dedicated login endpoint flow test.
- Reference-data service internals: standalone service-level coverage beyond controller wiring.
- Notification service internals: standalone service-level behavior coverage beyond controller wiring.
