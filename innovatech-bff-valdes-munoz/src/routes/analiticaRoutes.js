const express = require('express');
const router = express.Router();
const { httpClient } = require('../services/httpClient');

const authHeader = (req) => ({ Authorization: req.headers.authorization });

router.get('/metricas', async (req, res, next) => {
  try {
    const data = await httpClient.analitica.getAllMetrics(authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/metricas/proyecto/:projectId', async (req, res, next) => {
  try {
    const data = await httpClient.analitica.getProjectMetrics(req.params.projectId, authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/resumen', async (req, res, next) => {
  try {
    const data = await httpClient.analitica.getSummary(authHeader(req));
    res.status(200).json(data);
  } catch (err) { next(err); }
});

module.exports = router;