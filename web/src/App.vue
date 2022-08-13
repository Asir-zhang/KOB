<template>
  <div>

    <div>Bot昵称 :{{ bot_name }}</div>
    <div>Bot战力 :{{ bot_rating }}</div>
  </div>
  <router-view></router-view>
</template>


<script>
import $ from 'jquery'
import { ref } from 'vue';

export default {
  name: "App",
  setup: () => {
    let bot_name = ref("");
    let bot_rating = ref("");

    // 访问后端
    $.ajax({
      url: "http://localhost:8081/pk/get_bot_info/",
      type: "get",
      success: resp => {
        // console.log(resp);
        bot_name.value = resp.name;
        bot_rating.value = resp.rating;
      }
    });
    return {
      bot_name,
      bot_rating
    }
  }
}
</script>

<style>
body{
  background: url(@/assets/back.png);
  background-size: cover;
}
</style>
