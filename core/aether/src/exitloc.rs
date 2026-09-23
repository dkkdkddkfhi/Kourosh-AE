//! Exit-location policy compatible with Aether v2.1.0's `--exit-loc` flag.
//!
//! The policy is deliberately pure: the Android layer supplies the country
//! learned from its exit-IP lookup, while this module only decides whether the
//! country is acceptable. That keeps geolocation out of the tunnel core and
//! makes the behaviour deterministic and testable.

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExitLocationPolicy {
    allowed: Vec<String>,
    denied: Vec<String>,
}

impl ExitLocationPolicy {
    /// Parses values such as `!IR,AZ,RU` or `DE,SE`.
    ///
    /// Country codes are normalised to uppercase and malformed tokens are
    /// ignored. An empty value disables the policy.
    pub fn parse(raw: &str) -> Option<Self> {
        let mut allowed = Vec::new();
        let mut denied = Vec::new();
        for token in raw.split([',', ';', ' ', '\n', '\r', '\t']) {
            let token = token.trim();
            if token.is_empty() {
                continue;
            }
            let (is_denied, code) = token
                .strip_prefix('!')
                .map(|value| (true, value))
                .unwrap_or((false, token));
            let code = code.trim().to_ascii_uppercase();
            if code.len() != 2 || !code.bytes().all(|byte| byte.is_ascii_alphabetic()) {
                continue;
            }
            let target = if is_denied { &mut denied } else { &mut allowed };
            if !target.iter().any(|item| item == &code) {
                target.push(code);
            }
        }
        if allowed.is_empty() && denied.is_empty() {
            None
        } else {
            Some(Self { allowed, denied })
        }
    }

    pub fn accepts(&self, country: &str) -> bool {
        let country = country.trim().to_ascii_uppercase();
        if country.len() != 2 {
            return false;
        }
        if self.denied.iter().any(|item| item == &country) {
            return false;
        }
        self.allowed.is_empty() || self.allowed.iter().any(|item| item == &country)
    }

    pub fn is_enabled(&self) -> bool {
        !(self.allowed.is_empty() && self.denied.is_empty())
    }
}

#[cfg(test)]
mod tests {
    use super::ExitLocationPolicy;

    #[test]
    fn supports_deny_list() {
        let policy = ExitLocationPolicy::parse("!ir, az, RU").unwrap();
        assert!(!policy.accepts("IR"));
        assert!(!policy.accepts("az"));
        assert!(!policy.accepts("RU"));
        assert!(policy.accepts("DE"));
    }

    #[test]
    fn supports_allow_list() {
        let policy = ExitLocationPolicy::parse("DE, se").unwrap();
        assert!(policy.accepts("de"));
        assert!(policy.accepts("SE"));
        assert!(!policy.accepts("NL"));
    }

    #[test]
    fn ignores_invalid_tokens_and_empty_values() {
        assert!(ExitLocationPolicy::parse("").is_none());
        let policy = ExitLocationPolicy::parse("!, D, DE, DE").unwrap();
        assert!(policy.accepts("DE"));
        assert!(!policy.accepts("NL"));
    }
}

pub fn country_allowed(policy: Option<&str>, country: &str) -> bool {
    policy
        .and_then(ExitLocationPolicy::parse)
        .map(|value| value.accepts(country))
        .unwrap_or(true)
}
