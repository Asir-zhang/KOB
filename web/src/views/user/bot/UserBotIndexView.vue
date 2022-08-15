<template>
    <ContentField>我的Bot</ContentField>
</template>

<script>
import ContentField from "@/components/ContendField.vue"
import $ from 'jquery'
import{ useStore } from 'vuex'

export default {
    components:{
        ContentField
    },
    setup(){
        const store = useStore();
        // $.ajax({
        //     url: "http://localhost:8081/user/bot/add/",
        //     type: "post",
        //     data: {
        //         title: "my bot3",
        //         description: "好 bot",
        //         content: "Syste.out.println()",
        //     },
        //     headers: {
        //         Authorization: "Asir "+store.state.user.token,
        //     },
        //     success(resp){
        //         console.log(resp);
        //     },
        //     error(resp){
        //         console.log(resp);
        //     }
        // })

        // $.ajax({
        //     url: "http://localhost:8081/user/bot/remove/",
        //     type: "post",
        //     data: {
        //         bot_id: "6",
        //     },
        //     headers: {
        //         Authorization: "Asir "+store.state.user.token,
        //     },
        //     success(resp){
        //         console.log(resp);
        //     },
        //     error(resp){
        //         console.log(resp);
        //     }
        // })

        // $.ajax({
        //     url: "http://localhost:8081/user/bot/update/",
        //     type: "post",
        //     data: {
        //         bot_id: "3",
        //         title: "my bot改",
        //         description: "坏 bot",
        //         content: "Syste.out.println()",
        //     },
        //     headers: {
        //         Authorization: "Asir "+store.state.user.token,
        //     },
        //     success(resp){
        //         console.log(resp);
        //     },
        //     error(resp){
        //         console.log(resp);
        //     }
        // })

        $.ajax({
            url: "http://localhost:8081/user/bot/getlist/",
            type: "get",
            headers: {
                Authorization: "Asir "+store.state.user.token,
            },
            success(resp){
                console.log(resp);
            },
            error(resp){
                console.log(resp);
            }
        })
    },
}
</script>

<style scoped>

</style>