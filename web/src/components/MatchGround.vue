<template>
    <div class="matchground">
        <div class="row">
            <!-- 左边用户 -->
            <div class="col-4">
                <div class="user-photo">
                    <img :src="$store.state.user.photo" alt="">
                </div>
                <div class="user-username">
                    {{ $store.state.user.username }}
                </div>
            </div>
            <!-- 设置可选择机器人 -->
            <div class="col-4">
                <div class="user-select-bot">
                    <select  v-model="select_bot" class="form-select" aria-label="Default select example">
                        <option value="-1" selected>亲自上阵</option>
                        <option v-for="bot in bots" :key="bot.id" :value="bot.id">
                            {{ bot.title }}
                        </option>
                    </select>
                </div>
            </div>
            <!-- 右边用户 -->
            <div class="col-4">
                <div class="user-photo">
                    <img :src="$store.state.pk.opponent_photo" alt="">
                </div>
                <div class="user-username">
                    {{ $store.state.pk.opponent_username }}
                </div>
            </div>
            <!-- 匹配按钮 -->
            <div class="col-12" style="text-align: center; padding-top: 15vh;">
                <button @click="click_match_btn" class="btn btn-warning btn-lg">{{ match_btn_info }}</button>
            </div>
        </div>
    </div>
</template>

<script>
import { ref } from 'vue'
import { useStore } from 'vuex'
import $ from 'jquery'

export default {
    setup(){
        const store = useStore();
        let match_btn_info = ref("开始匹配");
        let bots = ref([]);
        let select_bot = ref("-1");

        const click_match_btn = () => {
            if(match_btn_info.value === "开始匹配"){
                match_btn_info.value = "取消匹配";
                console.log(select_bot.value);
                store.state.pk.socket.send(JSON.stringify({
                    event: "start-matching",
                    bot_id: select_bot.value,
                }));
            } else {
                match_btn_info.value = "开始匹配";
                store.state.pk.socket.send(JSON.stringify({
                    event: "stop-matching",
                }));
            }
        }

        const refresh_bots = () => {
            $.ajax({
                // url: "http://1.116.159.244:34567/user/bot/getlist/",
                url: "http://127.0.0.1:34567/user/bot/getlist/",
                type: "get",
                headers: {
                    Authorization: "Asir "+store.state.user.token,
                },
                success(resp){
                    bots.value = resp;
                }
            })
        }

        refresh_bots();     //从云端获取bots

        return {
            match_btn_info,
            click_match_btn,
            bots,
            select_bot,
        }
    }
}
</script>

<style scoped>
div.matchground {
    width: 60vw;
    height: 70vh;
    margin: 40px auto;
    background-color: rgba(50,50,50,0.5);
}
div.user-photo{
    text-align: center;
    margin-top: 50px;
}
div.user-photo > img {
    border-radius: 50%;
    width: 20vh;
}
div.user-username{
    text-align: center;
    font-size: 30px;
    font-weight: 600;
    color: white;
    padding-top: 1vh;
}
div.user-select-bot {
     padding-top: 20vh;
}
div.user-select-bot > select{
    width: 60%;
    margin: 0 auto;
}
</style>
