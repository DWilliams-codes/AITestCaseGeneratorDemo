import { Navigate, useParams } from 'react-router-dom';

/** Redirects the deprecated requirement URL to the canonical user-story route. */
export function LegacyRequirementRedirect() {
  const { requirementId = '' } = useParams();
  return <Navigate replace to={`/user-stories/${encodeURIComponent(requirementId)}`} />;
}
