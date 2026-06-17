const errorHandler = (err, req, res, next) => {
  console.error(`[BFF Error] ${err.message}`);

  if (err.response) {
    return res.status(err.response.status).json(err.response.data);
  }

  if (err.request) {
    return res.status(503).json({
      status: 503,
      error: 'Service Unavailable',
      message: 'No se pudo conectar con el microservicio. Intente más tarde.',
      timestamp: new Date().toISOString(),
    });
  }

  if (err.statusCode === 503) {
    return res.status(503).json({
      status: 503,
      error: 'Circuit Open',
      message: err.message,
      timestamp: new Date().toISOString(),
    });
  }

  return res.status(500).json({
    status: 500,
    error: 'Internal Server Error',
    message: err.message || 'Error interno del BFF',
    timestamp: new Date().toISOString(),
  });
};

module.exports = errorHandler;