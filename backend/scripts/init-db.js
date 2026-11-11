require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { pool } = require('../src/db');

(async () => {
  const dir = path.join(__dirname, '..', 'db');
  try {
    console.log('Creando tablas…');
    await pool.query(fs.readFileSync(path.join(dir, 'schema.sql'), 'utf8'));
    if (!process.argv.includes('--sin-datos')) {
      console.log('Cargando datos de prueba…');
      await pool.query(fs.readFileSync(path.join(dir, 'seed.sql'), 'utf8'));
    }
    const { rows } = await pool.query(`SELECT count(*)::int AS n FROM information_schema.tables WHERE table_schema = 'public'`);
    console.log(`✅ Listo: ${rows[0].n} tablas creadas.`);
  } catch (e) {
    console.error('❌ Error:', e.message);
    process.exitCode = 1;
  } finally {
    await pool.end();
  }
})();
