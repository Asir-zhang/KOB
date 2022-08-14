<template>
  <NavBar />
  <router-view></router-view>
</template>


<script>
import NavBar from '@/components/NavBar.vue'
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap/dist/js/bootstrap"

export default {
  components:{
    NavBar
  },
  setup() {
    
  }
}
</script>

<style>
body{
  background: url(@/assets/back.png);
  background-size: cover;
}
</style>
