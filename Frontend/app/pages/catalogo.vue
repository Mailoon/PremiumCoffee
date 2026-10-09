<script setup lang="ts">
const { data, error, status } = await useFetch('/api/categories', {
  query: { size: 5 }
})
</script>

<template>
  <section>
    <h1>Premium Coffee</h1>
    <p class="intro">
      Catalogo de cafe de especialidad con visor 3D de variantes.
    </p>

    <div class="panel">
      <h2>Estado de la cadena</h2>

      <p v-if="status === 'pending'">Consultando el backend…</p>

      <div v-else-if="error" class="error">
        <strong>No se pudo contactar el backend.</strong>
        <p>{{ error.message }}</p>
        <p class="hint">
          Revisa que el servicio <code>backend</code> este levantado y que
          <code>apiBase</code> apunte a <code>http://backend:8080</code>.
        </p>
      </div>

      <template v-else>
        <p>
          Responde <code>/api/categories</code> y devolvio
          <strong>{{ data?.totalElements ?? 0 }}</strong> categoria(s).
        </p>
        <ul v-if="data?.content?.length">
          <li v-for="c in data.content" :key="c.id">{{ c.name }}</li>
        </ul>
      </template>
    </div>
  </section>
</template>

<style scoped>
h1 {
  margin: 0 0 0.25rem;
  font-size: 2rem;
}

.intro {
  color: var(--muted);
  margin: 0 0 2rem;
}

.panel {
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
}

.panel h2 {
  margin-top: 0;
  font-size: 0.95rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--muted);
}

.error {
  color: #f0a5a5;
}

.hint {
  color: var(--muted);
  font-size: 0.9rem;
}

code {
  background: #2c2320;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  font-size: 0.85em;
}
</style>