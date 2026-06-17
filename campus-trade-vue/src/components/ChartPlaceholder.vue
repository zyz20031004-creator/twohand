<template>
  <div class="chart">
    <!-- 该文件已经废弃-->
    <!-- y 轴 -->
    <div class="y-axis"></div>
    <!-- x 轴 -->
    <div class="x-axis"></div>

    <!-- 内容区（折线/柱状） -->
    <div class="plot">
      <!-- 折线图占位：用一条折线（纯线框） -->
      <svg v-if="type === 'line'" class="svg" viewBox="0 0 100 60" preserveAspectRatio="none">
        <!-- 虚拟折线 -->
        <polyline
          points="5,45 20,35 35,40 50,25 65,30 80,18 95,22"
          fill="none"
          stroke="#333"
          stroke-width="1.5"
        />
        <!-- 虚拟点位 -->
        <circle v-for="(p, i) in linePoints" :key="i" :cx="p.x" :cy="p.y" r="1.8" fill="#333" />
      </svg>

      <!-- 柱状图占位：用矩形柱子（纯线框） -->
      <div v-else class="bars">
        <div v-for="(h, i) in barHeights" :key="i" class="bar" :style="{ height: h + '%' }"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 低保真图表占位组件
 * type=line：折线图占位
 * type=bar ：柱状图占位
 */
const props = defineProps<{
  type: "line" | "bar";
}>();

/** 折线点位（配合 svg circle） */
const linePoints = [
  { x: 5, y: 45 },
  { x: 20, y: 35 },
  { x: 35, y: 40 },
  { x: 50, y: 25 },
  { x: 65, y: 30 },
  { x: 80, y: 18 },
  { x: 95, y: 22 },
];

/** 柱状高度（百分比） */
const barHeights = [40, 65, 30, 55, 70, 45, 60];
</script>

<style scoped>
/* 图表外框 */
.chart {
  position: relative;
  height: 260px;
  background: #fff;
  border: 1px dashed #bbb; /* 线框感 */
}

/* 坐标轴 */
.y-axis {
  position: absolute;
  left: 40px;
  top: 20px;
  bottom: 40px;
  width: 0;
  border-left: 2px solid #333;
}

.x-axis {
  position: absolute;
  left: 40px;
  right: 20px;
  bottom: 40px;
  height: 0;
  border-top: 2px solid #333;
}

/* 绘图区域 */
.plot {
  position: absolute;
  left: 42px;
  top: 20px;
  right: 20px;
  bottom: 42px;
  padding: 10px;
}

/* 折线图 svg */
.svg {
  width: 100%;
  height: 100%;
}

/* 柱状图 */
.bars {
  height: 100%;
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.bar {
  flex: 1;
  border: 2px solid #333;    /* 只有描边 */
  background: transparent;   /* 不上色 */
}
</style>
