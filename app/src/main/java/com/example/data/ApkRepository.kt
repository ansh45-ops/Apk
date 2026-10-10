package com.example.data

import kotlinx.coroutines.flow.Flow

class ApkRepository(private val dao: ApkBuildDao) {
    val allBuilds: Flow<List<ApkBuildEntity>> = dao.getAllBuilds()

    suspend fun getBuildCount(): Int = dao.getBuildCount()

    suspend fun getBuildById(id: Long): ApkBuildEntity? = dao.getBuildById(id)

    suspend fun insertBuild(build: ApkBuildEntity): Long = dao.insertBuild(build)

    suspend fun deleteBuildById(id: Long) = dao.deleteBuildById(id)
}
