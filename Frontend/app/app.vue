<template>
  <div class="shell">
    <header class="shell__header">
      <NuxtLink to="/" class="brand">Premium Coffee</NuxtLink>
      <nav>
        <NuxtLink to="/catalogo">Catalogo</NuxtLink>
      </nav>
    </header>

    <main class="shell__main">
      <NuxtPage />
    </main>
  </div>
</template>

<style>
:root {
  color-scheme: dark;
  --bg: #14100e;
  --surface: #1e1815;
  --border: #3a2f29;
  --text: #f2e9e3;
  --muted: #a3928a;
  --accent: #c98b52;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: var(--bg);
  color: var(--text);
  font-family: system-ui, -apple-system, 'Segoe UI', sans-serif;
}

.shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 2rem;
  border-bottom: 1px solid var(--border);
}

.brand {
  font-weight: 700;
  font-size: 1.15rem;
  color: var(--text);
  text-decoration: none;
}

.shell__header nav a {
  color: var(--muted);
  text-decoration: none;
  margin-left: 1.25rem;
}

.shell__header nav a:hover {
  color: var(--accent);
}

.shell__main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 2rem;
}
</style>