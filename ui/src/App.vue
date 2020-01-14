<template>
  <div class="container">
    <!-- navigation -->
    <main-nav />
    <!-- route outlet -->
    <!-- component matched by the route will render here -->
    <router-view />
    <!-- footer -->
    <main-footer />
  </div>
</template>

<script>
  import MainNav from '@/components/layout/MainNav.vue'

  export default {
    name: 'App',
    components: {
      MainNav,
    }
  }
</script>

<style>
</style>
