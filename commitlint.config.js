module.exports = {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "scope-enum": [2, "always", ["common", "paper", "velocity", "docs", "build"]],
    "scope-empty": [2, "never"],
  },
};
