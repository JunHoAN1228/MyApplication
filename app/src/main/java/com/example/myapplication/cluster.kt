package com.example.myapplication

import smile.clustering.DBSCAN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.locationtech.proj4j.BasicCoordinateTransform
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import smile.math.distance.EuclideanDistance


data class DataPoint(val latitude: Double, val longitude: Double, val altitude: Double, val day: Int)

suspend fun processData(inputDataList: List<DataPoint>): List<DataPoint> = withContext(Dispatchers.Default) {
    // 좌표 변환

    val crsFactory = CRSFactory()
    val srcCRS = crsFactory.createFromName("EPSG:4326") // WGS84 좌표계
    val dstCRS = crsFactory.createFromName("EPSG:3857") // Web Mercator 좌표계
    val ctFactory = CoordinateTransformFactory()
    val transform = ctFactory.createTransform(srcCRS, dstCRS) as BasicCoordinateTransform

    val transformedData = inputDataList.map { dataPoint ->
        val srcCoord = ProjCoordinate(dataPoint.longitude, dataPoint.latitude) // 경도, 위도 순서
        val dstCoord = ProjCoordinate()
        transform.transform(srcCoord, dstCoord)
        DataPoint(dstCoord.y, dstCoord.x, dataPoint.altitude, dataPoint.day) // 위도, 경도 순서로 변경
    }

    // 데이터 포인트를 double 배열로 변환
    val transformedDataArray = transformedData.map { doubleArrayOf(it.latitude, it.longitude) }.toTypedArray()

    // DBSCAN 클러스터링
    val eps = 50.0
    val minSamples = 30
    val dbscan = DBSCAN.fit(transformedDataArray, EuclideanDistance(), minSamples,eps)

    // 클러스터 레이블 정보 출력
    dbscan.y.forEachIndexed { index, label ->
        println("Data point $index is in cluster $label")
    }

    // 클러스터에 대한 일 수 계산
    val clusterDays = mutableMapOf<Int, MutableSet<Int>>()
    transformedData.forEachIndexed { index, dataPoint ->
        val cluster = dbscan.y[index]
        if (cluster >= 0) {
            if (clusterDays.containsKey(cluster)) {
                clusterDays[cluster]?.add(dataPoint.day)
            } else {
                clusterDays[cluster] = mutableSetOf(dataPoint.day)
            }
        }
    }

    // clusterDays의 내용을 출력합니다.
    clusterDays.forEach { (cluster, days) ->
        println("Cluster $cluster has days: $days")
    }

    // 7일 이상 방문한 클러스터 선택
    val selectedClusters = clusterDays.filter { entry ->
        entry.key != 2147483647 && entry.value.size >= 5
    }.keys
    println("Selected Clusters: $selectedClusters")

    // 선택된 클러스터의 데이터 포인트 반환
    val selectedDataPoints = transformedData.filterIndexed { index, _ -> selectedClusters.contains(dbscan.y[index]) }

    // 선택된 데이터 포인트를 WGS84 좌표계로 변환
    val inverseTransform = ctFactory.createTransform(dstCRS, srcCRS) as BasicCoordinateTransform

    return@withContext selectedDataPoints.map { dataPoint ->
        val srcCoord = ProjCoordinate(dataPoint.longitude, dataPoint.latitude) // 경도, 위도 순서
        val dstCoord = ProjCoordinate()
        inverseTransform.transform(srcCoord, dstCoord)
        DataPoint(dstCoord.y, dstCoord.x, dataPoint.altitude, dataPoint.day) // 위도, 경도 순서로 변경
    }
}


