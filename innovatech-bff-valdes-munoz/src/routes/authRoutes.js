const express = require('express');
const router = express.Router();
const { httpClient } = require('../services/httpClient');

router.post('/register', async (req, res, next) => {
  try {
    const data = await httpClient.auth.register(req.body);
    res.status(201).json(data);
  } catch (err) { next(err); }
});

router.post('/login', async (req, res, next) => {
  try {
    const data = await httpClient.auth.login(req.body);
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.post('/validate', async (req, res, next) => {
  try {
    const token = req.body.token || (req.headers.authorization || '').replace('Bearer ', '');
    const data = await httpClient.auth.validate(token);
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/users', async (req, res, next) => {
  try {
    const headers = { Authorization: req.headers.authorization };
    const data = await httpClient.auth.getUsers(headers);
    res.status(200).json(data);
  } catch (err) { next(err); }
});

router.get('/users/:id', async (req, res, next) => {
  try {
    const headers = { Authorization: req.headers.authorization };
    const data = await httpClient.auth.getUserById(req.params.id, headers);
    res.status(200).json(data);
  } catch (err) { next(err); }
});

module.exports = router;