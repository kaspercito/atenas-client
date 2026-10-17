// Crea la version "AtenasClient-1.8.9" en .minecraft/versions usando Forge 1.8.9 + el jar del mod.
// Uso:  node instalar-version.mjs [ruta/al/AtenasClient-1.0.1.jar] [--mc "C:\ruta\.minecraft"] [--perfil]
import fs from "node:fs";
import path from "node:path";
import os from "node:os";

const args = process.argv.slice(2);
const flag = (n) => args.includes(n);
const opt = (n) => { const i = args.indexOf(n); return i >= 0 ? args[i + 1] : null; };

function defaultMc() {
  if (process.platform === "win32") return path.join(process.env.APPDATA || os.homedir(), ".minecraft");
  if (process.platform === "darwin") return path.join(os.homedir(), "Library", "Application Support", "minecraft");
  return path.join(os.homedir(), ".minecraft");
}

const mc = opt("--mc") || defaultMc();
const jarArg = args.find((a, i) => a.toLowerCase().endsWith(".jar") && args[i - 1] !== "--mc");
const jar = jarArg || path.join("build", "libs", "AtenasClient-1.0.1.jar");
const NEW_ID = "AtenasClient-1.8.9";

function fail(msg) { console.error("\n[X] " + msg); process.exit(1); }

if (!fs.existsSync(jar)) fail(`No encuentro el jar: ${jar}\n    Pasa la ruta: node instalar-version.mjs ruta\\AtenasClient-1.0.1.jar`);
const versionsDir = path.join(mc, "versions");
if (!fs.existsSync(versionsDir)) fail(`No existe ${versionsDir}. Usa --mc "ruta\\.minecraft".`);

// 1) Buscar la version de Forge 1.8.9 instalada
const forgeId = opt("--forge") || fs.readdirSync(versionsDir)
  .filter((d) => /^1\.8\.9-forge1\.8\.9-11\.15\.1\.\d+(-1\.8\.9)?$/.test(d)
    && fs.existsSync(path.join(versionsDir, d, d + ".json")))
  .sort().pop();
if (!forgeId) fail("No encuentro Forge 1.8.9 instalado en versions/. Instala Forge 1.8.9 (11.15.1.2318) primero.");
const forge = JSON.parse(fs.readFileSync(path.join(versionsDir, forgeId, forgeId + ".json"), "utf8"));

// 2) Copiar el jar como libreria
const ver = (path.basename(jar).match(/(\d+\.\d+\.\d+)/) || [null, "1.0.0"])[1];
const libName = `com.atenas:atenasclient:${ver}`;
const libDir = path.join(mc, "libraries", "com", "atenas", "atenasclient", ver);
fs.mkdirSync(libDir, { recursive: true });
fs.copyFileSync(jar, path.join(libDir, `atenasclient-${ver}.jar`));

// 3) Version nueva = copia de la de Forge + nuestra libreria
const v = JSON.parse(JSON.stringify(forge));
v.id = NEW_ID;
v.libraries = (v.libraries || []).filter((l) => !String(l.name).startsWith("com.atenas:atenasclient"));
v.libraries.push({ name: libName });
const outDir = path.join(versionsDir, NEW_ID);
fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, NEW_ID + ".json"), JSON.stringify(v, null, 2), "utf8");

// 4) Perfil opcional en el launcher oficial
if (flag("--perfil")) {
  const lp = path.join(mc, "launcher_profiles.json");
  if (fs.existsSync(lp)) {
    const prof = JSON.parse(fs.readFileSync(lp, "utf8"));
    prof.profiles = prof.profiles || {};
    prof.profiles["atenas-client"] = {
      name: "Atenas Client", type: "custom", lastVersionId: NEW_ID,
      created: new Date().toISOString(), lastUsed: new Date().toISOString(),
    };
    fs.writeFileSync(lp, JSON.stringify(prof, null, 2), "utf8");
    console.log("[ok] Perfil 'Atenas Client' agregado al launcher oficial.");
  }
}

console.log(`[ok] Version creada: ${path.join(outDir, NEW_ID + ".json")}`);
console.log(`[ok] Base: ${forgeId}  |  Libreria: ${libName}`);
console.log("\nAhora elige la version 'AtenasClient-1.8.9' en tu launcher.");
console.log("IMPORTANTE: quita AtenasClient*.jar de la carpeta mods si lo habias copiado (si no, se carga dos veces).");
