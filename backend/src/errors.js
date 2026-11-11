class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

const wrap = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

function errorHandler(err, req, res, _next) {
  if (err.code === '23505') return res.status(409).json({ message: 'Ese dato ya está registrado.' });

  if (err.code === '23514') return res.status(422).json({ message: 'Los datos no cumplen las reglas del sistema.' });
  if (err.name === 'MulterError') return res.status(422).json({ message: 'La foto no es válida (máx. 5 MB).' });
  const status = err.status || 500;
  if (status >= 500) console.error('[error]', err);
  res.status(status).json({ message: status >= 500 ? 'Error interno del servidor.' : err.message });
}

module.exports = { HttpError, wrap, errorHandler };
