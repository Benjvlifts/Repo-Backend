require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const authRoutes      = require('./routes/authRoutes');
const projectRoutes   = require('./routes/projectRoutes');
const resourceRoutes  = require('./routes/resourceRoutes');
const analiticaRoutes = require('./routes/analiticaRoutes');
const notifRoutes     = require('./routes/notifRoutes');
const errorHandler    = require('./middleware/errorHandler');
const { httpClient }  = require('./services/httpClient');

const app  = express();
const PORT = process.env.PORT || 3000;

app.use(helmet());
app.use(cors({ origin: '*', credentials: true }));
app.use(express.json());
app.use(morgan('combined'));
app.use(rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: { error: 'Demasiadas solicitudes. Intente más tarde.' },
}));

app.use('/api/auth',              authRoutes);
app.use('/api/projects',          projectRoutes);
app.use('/api/resources',         resourceRoutes);
app.use('/api/v1/analitica',      analiticaRoutes);
app.use('/api/v1/notificaciones', notifRoutes);

app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'innovatech-bff',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    circuitBreakers: httpClient.getBreakersStatus(),
  });
});

app.use(errorHandler);

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`✅ BFF Innovatech corriendo en http://localhost:${PORT}`);
    console.log(`   ms-auth      → ${process.env.MS_AUTH_URL}`);
    console.log(`   ms-proyectos → ${process.env.MS_PROYECTOS_URL}`);
    console.log(`   ms-recursos  → ${process.env.MS_RECURSOS_URL}`);
  });
}

module.exports = app;