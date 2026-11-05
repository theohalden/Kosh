import next from "eslint-config-next";
const config = [...next, { ignores: [".next/**", "node_modules/**", "scripts/**", "tests/**"] }];

export default config;
